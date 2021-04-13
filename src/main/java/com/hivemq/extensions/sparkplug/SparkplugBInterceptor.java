package com.hivemq.extensions.sparkplug;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.metrics.MetricsHolder;
import com.hivemq.extensions.sparkplug.topics.TopicStructure;
import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SparkplugBInterceptor implements PublishInboundInterceptor {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugBInterceptor.class);

    private @NotNull final MetricsHolder metricsHolder;

    private Map<Long, String> aliasToMetric = new HashMap<>();

    public SparkplugBInterceptor(final @NotNull MetricsHolder metricsHolder) {
        this.metricsHolder = metricsHolder;
    }

    @Override
    public void onInboundPublish(@NotNull PublishInboundInput publishInboundInput, @NotNull PublishInboundOutput publishInboundOutput) {
        //log.debug("Incoming publish from {}", publishInboundInput.getPublishPacket().getTopic());

        final PublishPacket publishPacket = publishInboundInput.getPublishPacket();
        final Optional<ByteBuffer> payload = publishPacket.getPayload();
        final String topic = publishPacket.getTopic();

        TopicStructure topicStructure = new TopicStructure(topic);
        if (payload.isPresent() && topicStructure.isValid()) {

            final ByteBuffer byteBuffer = payload.get();
            try {

                final SparkplugBProto.Payload spPayload = SparkplugBProto.Payload.parseFrom(byteBuffer);
                final List<SparkplugBProto.Payload.Metric> metricsList = spPayload.getMetricsList();
                for (SparkplugBProto.Payload.Metric metric : metricsList) {
                    aliasToMetric.put(metric.getAlias(), metric.getName());
                }
                //log.debug("generate metric from {}", topicStructure);
                generateMetricsFromMessage(topicStructure, metricsList);

            } catch (Exception e) {
                log.error("Could not parse MQTT payload to protobuf", e);
            }
        }
    }

    private void generateMetricsFromMessage(TopicStructure topicStructure, List<SparkplugBProto.Payload.Metric> metricsList) {
        log.debug("Message type & structure {} ", topicStructure );
        switch (topicStructure.getMessageType()) {
            case "NBIRTH": {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(),null).setValue(1);
                metricsHolder.getCurrentEonsOnline().inc();
                break;
            }
            case "NDEATH": {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), null).setValue(0);
                metricsHolder.getCurrentEonsOnline().dec();
                break;
            }
            case "DBIRTH": {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), topicStructure.getDeviceId()).setValue(1);
                metricsHolder.getCurrentDeviceOnline().inc();
                break;
            }
            case "DDEATH": {
                metricsHolder.getStatusMetrics(topicStructure.getEonId(), topicStructure.getDeviceId()).setValue(0);
                metricsHolder.getCurrentDeviceOnline().dec();
                break;
            }
            case "STATE": {
                metricsHolder.getStatusMetrics(topicStructure.getScadaId(), null).setValue(1);
                break;
            }
            case "DDATA":
            case "NDATA": {
                for (SparkplugBProto.Payload.Metric metric : metricsList) {
                        if( metric.getDatatype() == 3) {
                            metricsHolder.getDeviceInformationMetricsInt(topicStructure.getEonId(), topicStructure.getDeviceId(), metric.getName()).setValue(metric.getIntValue());
                        } else if( metric.getDatatype() == 4) {
                            metricsHolder.getDeviceInformationMetricsLong(topicStructure.getEonId(), topicStructure.getDeviceId(), metric.getName()).setValue(metric.getLongValue());
                        } else if( metric.getDatatype() == 10) {
                            metricsHolder.getDeviceInformationMetricsDouble(topicStructure.getEonId(), topicStructure.getDeviceId(), metric.getName()).setValue(metric.getDoubleValue());
                        } else {
                            metricsHolder.getDeviceDataMetrics(topicStructure.getEonId(), topicStructure.getDeviceId(), metric.getName()).setValue(metric.getFloatValue());
                        }
                }
                break;
            }
            default: {
                log.error("Unknown data target {} ", topicStructure );
            }

        }
    }

}
