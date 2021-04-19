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
package com.hivemq.extensions.sparkplug;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.common.collect.Sets;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.sparkplug.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.metrics.MetricsHolder;
import com.izettle.metrics.influxdb.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main entrypoint for sparkplug extension
 * starts influxdb reporter after validating the configuration
 * initializes the sparkplug interceptor
 *
 * @author Anja Helmbrecht-Schaar
 */
public class SparkplugExtensionMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugExtensionMain.class);
    private static final @NotNull HashSet<String> METER_FIELDS = Sets.newHashSet("count", "m1_rate", "m5_rate", "m15_rate", "mean_rate");
    private static final @NotNull HashSet<String> TIMER_FIELDS = Sets.newHashSet("count", "min", "max", "mean", "stddev", "p50", "p75", "p95", "p98", "p99", "p999", "m1_rate", "m5_rate", "m15_rate", "mean_rate");

    private @Nullable ScheduledReporter reporter;
    private @Nullable SparkplugConfiguration configuration;

    @Override
    public void extensionStart(@NotNull final ExtensionStartInput extensionStartInput, @NotNull final ExtensionStartOutput extensionStartOutput) {
        try {
            final @NotNull File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            //read & validate configuration
            if (!configurationValidated(extensionStartOutput, extensionHomeFolder)) {
                return;
            }
            if (configuration == null) {
                return;
            }
            final @Nullable InfluxDbSender sender = setupSender(configuration);
            if (sender == null) {
                extensionStartOutput.preventExtensionStartup("Couldn't create an influxdb sender. Please check that the configuration is correct");
                return;
            }

            reporter = setupReporter(Services.metricRegistry(), sender, configuration);
            reporter.start(configuration.getReportingInterval(), TimeUnit.SECONDS);

            initializeSparkplugMetricsInterceptor();

        } catch (Exception e) {
            log.warn("Start failed because of: ", e);
            extensionStartOutput.preventExtensionStartup("Start failed because of an exception");
        }
    }

    @Override
    public void extensionStop(@NotNull final ExtensionStopInput extensionStopInput, @NotNull final ExtensionStopOutput extensionStopOutput) {
        if (reporter != null) {
            reporter.stop();
        }
    }

    private boolean configurationValidated(@NotNull final ExtensionStartOutput extensionStartOutput, @NotNull final File extensionHomeFolder) {
        configuration = new SparkplugConfiguration(extensionHomeFolder);

        if (!configuration.readPropertiesFromFile()) {
            extensionStartOutput.preventExtensionStartup("Could not read influxdb properties");
            return false;
        }
        if (!configuration.validateConfiguration()) {
            extensionStartOutput.preventExtensionStartup("At least one mandatory property not set");
            return false;
        }
        return true;
    }

    private void initializeSparkplugMetricsInterceptor() {
        final MetricsHolder metricsHolder = new MetricsHolder(Services.metricRegistry());
        final SparkplugBInterceptor sparkplugBInterceptor = new SparkplugBInterceptor(metricsHolder, configuration);
        Services.initializerRegistry().setClientInitializer((initializerInput, clientContext) -> clientContext.addPublishInboundInterceptor(sparkplugBInterceptor));
    }

    @NotNull
    private ScheduledReporter setupReporter(@NotNull final MetricRegistry metricRegistry, @NotNull final InfluxDbSender sender, @NotNull final SparkplugConfiguration configuration) {
        checkNotNull(metricRegistry, "MetricRegistry for influxdb must not be null");
        checkNotNull(sender, "InfluxDbSender for influxdb must not be null");
        checkNotNull(configuration, "Configuration for influxdb must not be null");

        final Map<String, String> tags = configuration.getTags();

        return InfluxDbReporter.forRegistry(metricRegistry)
                .withTags(tags)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .groupGauges(false)
                .skipIdleMetrics(false)
                .includeMeterFields(METER_FIELDS)
                .includeTimerFields(TIMER_FIELDS)
                .build(sender);
    }

    @Nullable
    private InfluxDbSender setupSender(@NotNull final SparkplugConfiguration configuration) {
        checkNotNull(configuration, "Configuration for influxdb must not be null");

        final String host = configuration.getHost();
        final int port = configuration.getPort();
        final String protocol = configuration.getProtocol();
        final String database = configuration.getDatabase();
        final String auth = configuration.getAuth();
        final int connectTimeout = configuration.getConnectTimeout();
        final String prefix = configuration.getPrefix();

        // cloud
        final String bucket = configuration.getBucket();
        final String organization = configuration.getOrganization();

        InfluxDbSender sender = null;

        try {
            switch (configuration.getMode()) {
                case "http":
                    log.info("Creating InfluxDB HTTP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbHttpSender(protocol, host, port, database, auth, TimeUnit.SECONDS, connectTimeout, connectTimeout, prefix);
                    break;
                case "tcp":
                    log.info("Creating InfluxDB TCP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbTcpSender(host, port, connectTimeout, database, prefix);
                    break;
                case "udp":
                    log.info("Creating InfluxDB UDP sender for server {}:{} and database {}", host, port, database);
                    sender = new InfluxDbUdpSender(host, port, connectTimeout, database, prefix);
                    break;
                case "cloud":
                    log.info("Creating InfluxDB Cloud sender for endpoint {}, bucket {}, organization {}", host, bucket, organization);
                    checkNotNull(bucket, "Bucket name must be defined in cloud mode");
                    checkNotNull(organization, "Organization must be defined in cloud mode");
                    sender = new InfluxDbCloudSender(protocol, host, port, auth, TimeUnit.SECONDS, connectTimeout, connectTimeout, prefix, organization, bucket);
                    break;

            }
        } catch (Exception ex) {
            log.error("Not able to start InfluxDB sender, please check your configuration: {}", ex.getMessage());
            log.debug("Original Exception: ", ex);
        }

        return sender;
    }


}
