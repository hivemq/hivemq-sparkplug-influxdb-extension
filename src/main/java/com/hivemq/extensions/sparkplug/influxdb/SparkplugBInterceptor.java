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

import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extensions.sparkplug.influxdb.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.influxdb.metrics.MetricsHolder;
import com.hivemq.extensions.sparkplug.influxdb.topics.TopicStructure;
import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hivemq.extensions.sparkplug.influxdb.topics.MessageType.STATE;

/**
 * Interceptor for incoming MQTT publish messages that processes Sparkplug B payloads.
 * <p>
 * This interceptor is responsible for:
 * <ul>
 *     <li>Validating incoming MQTT topics against the Sparkplug topic structure</li>
 *     <li>Parsing Sparkplug B protobuf payloads to extract metrics</li>
 *     <li>Maintaining an alias-to-metric-name mapping for efficient data transmission</li>
 *     <li>Registering metrics in the {@link MetricsHolder} for reporting to InfluxDB</li>
 * </ul>
 * <p>
 * The interceptor handles all Sparkplug message types including BIRTH, DEATH, DATA,
 * and STATE messages, updating metrics accordingly.
 *
 * @author David Sondermann
 * @see TopicStructure
 * @see MetricsHolder
 */
public class SparkplugBInterceptor implements PublishInboundInterceptor {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(SparkplugBInterceptor.class);

    /**
     * Maps Sparkplug metric aliases to their full metric names.
     * Sparkplug uses aliases to reduce message size after initial BIRTH messages.
     */
    private final @NotNull Map<Long, String> aliasToMetric = new HashMap<>();

    /**
     * Holder for managing and accessing Sparkplug metrics.
     */
    private final @NotNull MetricsHolder metricsHolder;

    /**
     * The expected Sparkplug version namespace (e.g., "spBv1.0").
     */
    private final @NotNull String sparkplugVersion;

    /**
     * Constructs a new SparkplugBInterceptor.
     *
     * @param metricsHolder the holder for managing Sparkplug metrics
     * @param configuration the extension configuration containing the Sparkplug version
     */
    public SparkplugBInterceptor(
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull SparkplugConfiguration configuration) {
        this.metricsHolder = metricsHolder;
        this.sparkplugVersion = configuration.getSparkplugVersion();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Processes incoming publish messages and extracts Sparkplug B metrics.
     * <p>
     * If the topic matches the configured Sparkplug version namespace and has a valid
     * Sparkplug topic structure, the protobuf payload is parsed and metrics are extracted
     * and registered with the metrics holder.
     */
    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Incoming publish from {}", publishInboundInput.getPublishPacket().getTopic());
        }
        final var publishPacket = publishInboundInput.getPublishPacket();
        final var topic = publishPacket.getTopic();
        final var payload = publishPacket.getPayload();
        final var topicStructure = new TopicStructure(topic);
        if (payload.isPresent() && topicStructure.isValid(sparkplugVersion)) {
            // it's a Sparkplug publish
            final var byteBuffer = payload.get();
            try {
                final var spPayload = SparkplugBProto.Payload.parseFrom(byteBuffer);
                final var metricsList = spPayload.getMetricsList();
                for (final var metric : metricsList) {
                    aliasToMetric.put(metric.getAlias(), metric.getName());
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Add Metric Mapping (Alias={}, MetricName={})", metric.getAlias(), metric.getName());
                    }
                }
                generateMetricsFromMessage(topicStructure, metricsList);
            } catch (final Exception e) {
                LOG.error("Could not parse MQTT payload to protobuf", e);
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("This might not be a Sparkplug topic structure: {}", topicStructure);
            }
        }
    }

    /**
     * Generates metrics from a Sparkplug message based on its type.
     * <p>
     * For STATE messages, updates the SCADA host status metric.
     * For all other message types, delegates to {@link #generateMetricForEdgesAndDevices}.
     *
     * @param topicStructure the parsed Sparkplug topic structure
     * @param metricsList    the list of metrics from the Sparkplug payload
     */
    private void generateMetricsFromMessage(
            final @NotNull TopicStructure topicStructure,
            final @NotNull List<SparkplugBProto.Payload.Metric> metricsList) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sparkplug Message type & structure {} ", topicStructure);
        }
        if (topicStructure.getScadaId() != null && STATE == topicStructure.getMessageType()) {
            metricsHolder.getStatusMetrics(topicStructure.getScadaId(), null).setValue(1);
        } else {
            generateMetricForEdgesAndDevices(topicStructure, metricsList);
        }
    }

    /**
     * Generates metrics for edge nodes and devices based on the Sparkplug message type.
     * <p>
     * Handles the following message types:
     * <ul>
     *     <li><b>NBIRTH</b> - Sets edge node status to online and increments online counter</li>
     *     <li><b>NDEATH</b> - Sets edge node status to offline and decrements online counter</li>
     *     <li><b>DBIRTH</b> - Sets device status to online and increments device counter</li>
     *     <li><b>DDEATH</b> - Sets device status to offline and decrements device counter</li>
     *     <li><b>NDATA/DDATA</b> - Extracts and registers individual metric values</li>
     * </ul>
     *
     * @param topicStructure the parsed Sparkplug topic structure containing edge node and device IDs
     * @param metricsList    the list of metrics from the Sparkplug payload
     */
    private void generateMetricForEdgesAndDevices(
            final @NotNull TopicStructure topicStructure,
            final @NotNull List<SparkplugBProto.Payload.Metric> metricsList) {
        if (topicStructure.getEonId() == null) {
            LOG.error("Edge Node Id is null - Sparkplug Message structure {} ", topicStructure);
            return;
        }
        switch (topicStructure.getMessageType()) {
            case NBIRTH: {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), null).setValue(1);
                metricsHolder.getCurrentEonsOnline().inc();
                break;
            }
            case NDEATH: {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), null).setValue(0);
                metricsHolder.getCurrentEonsOnline().dec();
                break;
            }
            case DBIRTH: {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), topicStructure.getDeviceId()).setValue(1);
                metricsHolder.getCurrentDeviceOnline().inc();
                break;
            }
            case DDEATH: {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), topicStructure.getDeviceId()).setValue(0);
                metricsHolder.getCurrentDeviceOnline().dec();
                break;
            }
            case DDATA:
            case NDATA: {
                for (final var metric : metricsList) {
                    final var alias = metric.getAlias();
                    final var metricName = aliasToMetric.get(alias);
                    if (metric.hasIntValue()) {
                        metricsHolder.getDeviceInformationMetricsInt(topicStructure.getEonId(),
                                topicStructure.getDeviceId(),
                                metricName).setValue(metric.getIntValue());
                    } else if (metric.hasLongValue()) {
                        metricsHolder.getDeviceInformationMetricsLong(topicStructure.getEonId(),
                                topicStructure.getDeviceId(),
                                metricName).setValue(metric.getLongValue());
                    } else if (metric.hasDoubleValue()) {
                        metricsHolder.getDeviceInformationMetricsDouble(topicStructure.getEonId(),
                                topicStructure.getDeviceId(),
                                metricName).setValue(metric.getDoubleValue());
                    } else if (metric.hasBooleanValue()) {
                        metricsHolder.getDeviceInformationMetricsBoolean(topicStructure.getEonId(),
                                topicStructure.getDeviceId(),
                                metricName).setValue(metric.getBooleanValue());
                    } else if (metric.hasFloatValue()) {
                        metricsHolder.getDeviceDataMetrics(topicStructure.getEonId(),
                                topicStructure.getDeviceId(),
                                metricName).setValue(metric.getFloatValue());
                    }
                }
                break;
            }
            default: {
                LOG.error("Unknown Sparkplug Message Type: {} ", topicStructure);
            }
        }
    }
}
