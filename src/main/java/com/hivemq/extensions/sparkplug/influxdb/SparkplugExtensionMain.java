/*
 * Copyright 2021-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.sparkplug.influxdb;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.sparkplug.influxdb.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.influxdb.metrics.MetricsHolder;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import com.izettle.metrics.influxdb.InfluxDbSender;
import com.izettle.metrics.influxdb.InfluxDbTcpSender;
import com.izettle.metrics.influxdb.InfluxDbUdpSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Main entrypoint for Sparkplug extension.
 * Starts InfluxDB reporter after validating the configuration.
 * Initializes the Sparkplug interceptor.
 *
 * @author David Sondermann
 */
public class SparkplugExtensionMain implements ExtensionMain {

    private static final @NotNull Set<String> METER_FIELDS =
            Set.of("count", "m1_rate", "m5_rate", "m15_rate", "mean_rate");
    private static final @NotNull Set<String> TIMER_FIELDS = Set.of("count",
            "min",
            "max",
            "mean",
            "stddev",
            "p50",
            "p75",
            "p95",
            "p98",
            "p99",
            "p999",
            "m1_rate",
            "m5_rate",
            "m15_rate",
            "mean_rate");

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(SparkplugExtensionMain.class);

    private @Nullable ScheduledReporter reporter;

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            final var extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            // read & validate configuration
            final var configuration = configurationValidated(extensionStartOutput, extensionHomeFolder);
            if (configuration == null) {
                return;
            }
            final var sender = setupSender(configuration);
            if (sender == null) {
                extensionStartOutput.preventExtensionStartup(
                        "Couldn't create an InfluxDB sender. Please check that the configuration is correct");
                return;
            }
            reporter = setupReporter(Services.metricRegistry(), sender, configuration);
            reporter.start(configuration.getReportingInterval(), TimeUnit.SECONDS);
            initializeSparkplugMetricsInterceptor(configuration);
        } catch (final Exception e) {
            LOG.warn("Start failed because of: ", e);
            extensionStartOutput.preventExtensionStartup("Start failed because of an exception");
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        if (reporter != null) {
            reporter.stop();
        }
    }

    private @Nullable SparkplugConfiguration configurationValidated(
            final @NotNull ExtensionStartOutput extensionStartOutput,
            final @NotNull File extensionHomeFolder) {
        final var configuration = new SparkplugConfiguration(extensionHomeFolder);
        if (!configuration.readPropertiesFromFile()) {
            extensionStartOutput.preventExtensionStartup("Could not read InfluxDB properties");
            return null;
        }
        if (!configuration.validateConfiguration()) {
            extensionStartOutput.preventExtensionStartup("At least one mandatory property not set");
            return null;
        }
        return configuration;
    }

    /**
     * Initializes and registers the Sparkplug B interceptor with the HiveMQ client initializer registry.
     * <p>
     * The interceptor will be added to all connecting clients and will process incoming
     * Sparkplug B messages to extract and register metrics.
     *
     * @param configuration the extension configuration
     */
    private void initializeSparkplugMetricsInterceptor(final @NotNull SparkplugConfiguration configuration) {
        final var metricsHolder = new MetricsHolder(Services.metricRegistry());
        final var sparkplugBInterceptor = new SparkplugBInterceptor(metricsHolder, configuration);
        Services.initializerRegistry()
                .setClientInitializer((initializerInput, clientContext) -> clientContext.addPublishInboundInterceptor(
                        sparkplugBInterceptor));
    }

    /**
     * Creates and configures an InfluxDB reporter for the metric registry.
     * <p>
     * The reporter is configured to:
     * <ul>
     *     <li>Use tags from the configuration</li>
     *     <li>Convert rates to seconds</li>
     *     <li>Convert durations to milliseconds</li>
     *     <li>Include specific meter and timer fields</li>
     * </ul>
     *
     * @param metricRegistry the metric registry to report from
     * @param sender         the InfluxDB sender to use for reporting
     * @param configuration  the extension configuration
     * @return the configured scheduled reporter
     */
    private @NotNull ScheduledReporter setupReporter(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull InfluxDbSender sender,
            final @NotNull SparkplugConfiguration configuration) {
        Objects.requireNonNull(metricRegistry, "MetricRegistry for InfluxDB must not be null");
        Objects.requireNonNull(sender, "InfluxDbSender for InfluxDB must not be null");
        return InfluxDbReporter.forRegistry(metricRegistry)
                .withTags(configuration.getTags())
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .groupGauges(false)
                .skipIdleMetrics(false)
                .includeMeterFields(METER_FIELDS)
                .includeTimerFields(TIMER_FIELDS)
                .build(sender);
    }

    /**
     * Creates the appropriate InfluxDB sender based on the configured mode.
     * <p>
     * Supported modes:
     * <ul>
     *     <li><b>http</b> - HTTP sender for InfluxDB 1.x or 2.x compatibility mode</li>
     *     <li><b>tcp</b> - TCP sender for direct line protocol communication</li>
     *     <li><b>udp</b> - UDP sender for high-throughput, fire-and-forget scenarios</li>
     *     <li><b>cloud</b> - Cloud sender for InfluxDB Cloud with token authentication</li>
     * </ul>
     *
     * @param configuration the extension configuration
     * @return the configured InfluxDB sender, or {@code null} if creation failed
     */
    private @Nullable InfluxDbSender setupSender(final @NotNull SparkplugConfiguration configuration) {
        final var host = configuration.getHost();
        final var port = configuration.getPort();
        final var protocol = configuration.getProtocol();
        final var database = configuration.getDatabase();
        final var auth = configuration.getAuth();
        final var connectTimeout = configuration.getConnectTimeout();
        final var prefix = configuration.getPrefix();

        // cloud
        final var bucket = configuration.getBucket();
        final var organization = configuration.getOrganization();

        try {
            switch (configuration.getMode()) {
                case "http":
                    LOG.info("Creating InfluxDB HTTP sender for server {}:{} and database {}", host, port, database);
                    return new InfluxDbHttpSender(protocol,
                            host,
                            port,
                            database,
                            auth,
                            TimeUnit.SECONDS,
                            connectTimeout,
                            connectTimeout,
                            prefix);
                case "tcp":
                    LOG.info("Creating InfluxDB TCP sender for server {}:{} and database {}", host, port, database);
                    return new InfluxDbTcpSender(host, port, connectTimeout, database, prefix);
                case "udp":
                    LOG.info("Creating InfluxDB UDP sender for server {}:{} and database {}", host, port, database);
                    return new InfluxDbUdpSender(host, port, connectTimeout, database, prefix);
                case "cloud":
                    LOG.info("Creating InfluxDB Cloud sender for endpoint {}, bucket {}, organization {}",
                            host,
                            bucket,
                            organization);
                    Objects.requireNonNull(bucket, "Bucket name must be defined in cloud mode");
                    Objects.requireNonNull(organization, "Organization must be defined in cloud mode");
                    return new InfluxDbCloudSender(protocol,
                            host,
                            port,
                            auth,
                            TimeUnit.SECONDS,
                            connectTimeout,
                            connectTimeout,
                            prefix,
                            organization,
                            bucket);
            }
        } catch (final Exception e) {
            LOG.error("Not able to start InfluxDB sender, please check your configuration: {}", e.getMessage());
            LOG.debug("Original Exception: ", e);
        }
        return null;
    }
}
