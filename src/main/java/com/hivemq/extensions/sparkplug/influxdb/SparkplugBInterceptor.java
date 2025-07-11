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
 * Interceptor for incoming publishes
 * validates the topic structure as sparkplug topic
 * parses sparkplug payload
 * creates or add a metric via metricRegistry from sparkplug metric data object
 *
 * @author Anja Helmbrecht-Schaar
 */
public class SparkplugBInterceptor implements PublishInboundInterceptor {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugBInterceptor.class);
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull String sparkplugVersion;
    private final @NotNull Map<Long, String> aliasToMetric = new HashMap<>();

    public SparkplugBInterceptor(
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull SparkplugConfiguration configuration) {
        this.metricsHolder = metricsHolder;
        this.sparkplugVersion = configuration.getSparkplugVersion();
    }

    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {
        if (log.isTraceEnabled()) {
            log.trace("Incoming publish from {}", publishInboundInput.getPublishPacket().getTopic());
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
                    if (log.isTraceEnabled()) {
                        log.trace("Add Metric Mapping (Alias={}, MetricName={})", metric.getAlias(), metric.getName());
                    }
                }
                generateMetricsFromMessage(topicStructure, metricsList);
            } catch (final Exception e) {
                log.error("Could not parse MQTT payload to protobuf", e);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("This might not be a sparkplug topic structure: {}", topicStructure);
            }
        }
    }

    private void generateMetricsFromMessage(
            final @NotNull TopicStructure topicStructure,
            final @NotNull List<SparkplugBProto.Payload.Metric> metricsList) {
        if (log.isTraceEnabled()) {
            log.trace("Sparkplug Message type & structure {} ", topicStructure);
        }
        if (topicStructure.getScadaId() != null && STATE == topicStructure.getMessageType()) {
            metricsHolder.getStatusMetrics(topicStructure.getScadaId(), null).setValue(1);
        } else {
            generateMetricForEdgesAndDevices(topicStructure, metricsList);
        }
    }

    private void generateMetricForEdgesAndDevices(
            final @NotNull TopicStructure topicStructure,
            final @NotNull List<SparkplugBProto.Payload.Metric> metricsList) {
        if (topicStructure.getEonId() == null) {
            log.error("Edge Node Id is null - Sparkplug Message structure {} ", topicStructure);
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
                log.error("Unknown Sparkplug Message Type: {} ", topicStructure);
            }
        }
    }
}
