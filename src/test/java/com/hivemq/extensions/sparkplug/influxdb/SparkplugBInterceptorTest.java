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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.sparkplug.influxdb.configuration.SparkplugConfiguration;
import com.hivemq.extensions.sparkplug.influxdb.metrics.MetricsHolder;
import com.hivemq.extensions.sparkplug.influxdb.metrics.SettableBooleanGauge;
import com.hivemq.extensions.sparkplug.influxdb.metrics.SettableDoubleGauge;
import com.hivemq.extensions.sparkplug.influxdb.metrics.SettableIntGauge;
import com.hivemq.extensions.sparkplug.influxdb.metrics.SettableLongGauge;
import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SparkplugBInterceptorTest {

    private final @NotNull PublishInboundInput publishInboundInput = mock();
    private final @NotNull PublishInboundOutput publishInboundOutput = mock();
    private final @NotNull PublishPacket publishPacket = mock();
    private final @NotNull SparkplugConfiguration configuration = mock();

    private @NotNull MetricRegistry metricRegistry;
    private @NotNull MetricsHolder metricsHolder;
    private @NotNull SparkplugBInterceptor interceptor;

    @BeforeEach
    void setUp() {
        metricRegistry = new MetricRegistry();
        metricsHolder = new MetricsHolder(metricRegistry);

        when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);
        when(configuration.getSparkplugVersion()).thenReturn("spBv1.0");

        interceptor = new SparkplugBInterceptor(metricsHolder, configuration);
    }

    @Test
    void non_sparkplug_topic_ignored() {
        when(publishPacket.getTopic()).thenReturn("some/other/topic");
        when(publishPacket.getPayload()).thenReturn(Optional.empty());

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        assertThat(metricRegistry.getMetrics()).isEmpty();
    }

    @Test
    void sparkplug_topic_without_payload_ignored() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeNode");
        when(publishPacket.getPayload()).thenReturn(Optional.empty());

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        assertThat(metricRegistry.getMetrics()).isEmpty();
    }

    @Test
    void nbirth_sets_status_and_increments_eon_counter() {
        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("testMetric").setAlias(1).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", payload);

        assertThat(statusGaugeValue("sparkplug.edgeNode.status")).isEqualTo(1.0);
        assertThat(metricsHolder.getCurrentEonsOnline().getCount()).isEqualTo(1);
    }

    @Test
    void ndeath_sets_status_and_decrements_eon_counter() {
        // first NBIRTH
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("m").setAlias(1).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        // then NDEATH
        final var deathPayload = SparkplugBProto.Payload.newBuilder().build();
        publishWith("spBv1.0/group/NDEATH/edgeNode", deathPayload);

        assertThat(statusGaugeValue("sparkplug.edgeNode.status")).isEqualTo(0.0);
        assertThat(metricsHolder.getCurrentEonsOnline().getCount()).isEqualTo(0);
    }

    @Test
    void dbirth_sets_device_status_and_increments_device_counter() {
        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("m").setAlias(1).build())
                .build();
        publishWith("spBv1.0/group/DBIRTH/edgeNode/device1", payload);

        assertThat(statusGaugeValue("sparkplug.edgeNode.device1.status")).isEqualTo(1.0);
        assertThat(metricsHolder.getCurrentDeviceOnline().getCount()).isEqualTo(1);
    }

    @Test
    void ddeath_sets_device_status_and_decrements_device_counter() {
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("m").setAlias(1).build())
                .build();
        publishWith("spBv1.0/group/DBIRTH/edgeNode/device1", birthPayload);

        final var deathPayload = SparkplugBProto.Payload.newBuilder().build();
        publishWith("spBv1.0/group/DDEATH/edgeNode/device1", deathPayload);

        assertThat(statusGaugeValue("sparkplug.edgeNode.device1.status")).isEqualTo(0.0);
        assertThat(metricsHolder.getCurrentDeviceOnline().getCount()).isEqualTo(0);
    }

    @Test
    void state_sets_scada_status() {
        final var payload = SparkplugBProto.Payload.newBuilder().build();
        publishWith("spBv1.0/group/STATE/scadaHost", payload);

        assertThat(statusGaugeValue("sparkplug.scadaHost.status")).isEqualTo(1.0);
    }

    @Test
    void ndata_with_int_metric() {
        // NBIRTH to establish alias mapping
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("temperature").setAlias(10).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        // NDATA with int value
        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(10)
                        .setName("temperature")
                        .setIntValue(42)
                        .build())
                .build();
        publishWith("spBv1.0/group/NDATA/edgeNode", dataPayload);

        assertThat(metricRegistry.getMetrics()).containsKey("sparkplug.edgeNode.temperature");
        assertThat(metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.temperature")).isInstanceOf(SettableIntGauge.class);
        assertThat(((SettableIntGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.temperature")).getValue()).isEqualTo(42);
    }

    @Test
    void ndata_with_long_metric() {
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("uptime").setAlias(11).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(11)
                        .setName("uptime")
                        .setLongValue(123456789L)
                        .build())
                .build();
        publishWith("spBv1.0/group/NDATA/edgeNode", dataPayload);

        assertThat(metricRegistry.getMetrics().get("sparkplug.edgeNode.uptime")).isInstanceOf(SettableLongGauge.class);
        assertThat(((SettableLongGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.uptime")).getValue()).isEqualTo(123456789L);
    }

    @Test
    void ndata_with_double_metric() {
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("voltage").setAlias(12).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(12)
                        .setName("voltage")
                        .setDoubleValue(3.14)
                        .build())
                .build();
        publishWith("spBv1.0/group/NDATA/edgeNode", dataPayload);

        assertThat(metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.voltage")).isInstanceOf(SettableDoubleGauge.class);
        assertThat(((SettableDoubleGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.voltage")).getValue()).isEqualTo(3.14);
    }

    @Test
    void ndata_with_boolean_metric() {
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("active").setAlias(13).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(13)
                        .setName("active")
                        .setBooleanValue(true)
                        .build())
                .build();
        publishWith("spBv1.0/group/NDATA/edgeNode", dataPayload);

        assertThat(metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.active")).isInstanceOf(SettableBooleanGauge.class);
        assertThat(((SettableBooleanGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.active")).getValue()).isTrue();
    }

    @Test
    void ndata_with_float_metric() {
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("pressure").setAlias(14).build())
                .build();
        publishWith("spBv1.0/group/NBIRTH/edgeNode", birthPayload);

        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(14)
                        .setName("pressure")
                        .setFloatValue(1.5f)
                        .build())
                .build();
        publishWith("spBv1.0/group/NDATA/edgeNode", dataPayload);

        assertThat(metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.pressure")).isInstanceOf(SettableDoubleGauge.class);
        assertThat(((SettableDoubleGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.pressure")).getValue()).isEqualTo((double) 1.5f);
    }

    @Test
    void ddata_resolves_alias_from_prior_dbirth() {
        // DBIRTH establishes alias mapping
        final var birthPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("speed").setAlias(20).build())
                .build();
        publishWith("spBv1.0/group/DBIRTH/edgeNode/device1", birthPayload);

        // DDATA references alias only
        final var dataPayload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder()
                        .setAlias(20)
                        .setName("speed")
                        .setIntValue(100)
                        .build())
                .build();
        publishWith("spBv1.0/group/DDATA/edgeNode/device1", dataPayload);

        assertThat(metricRegistry.getMetrics()).containsKey("sparkplug.edgeNode.device1.speed");
        assertThat(((SettableIntGauge) metricRegistry.getMetrics()
                .get("sparkplug.edgeNode.device1.speed")).getValue()).isEqualTo(100);
    }

    @Test
    void ncmd_hits_unknown_message_type_branch() {
        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("cmd").setAlias(30).build())
                .build();
        publishWith("spBv1.0/group/NCMD/edgeNode", payload);

        // NCMD is not handled in the switch → default branch logs error, no status/counter metrics
        assertThat(metricRegistry.getMetrics()).doesNotContainKey("sparkplug.edgeNode.status");
        assertThat(metricsHolder.getCurrentEonsOnline().getCount()).isEqualTo(0);
    }

    @Test
    void dcmd_hits_unknown_message_type_branch() {
        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("cmd").setAlias(31).build())
                .build();
        publishWith("spBv1.0/group/DCMD/edgeNode/device1", payload);

        // DCMD is not handled in the switch → default branch logs error, no status/counter metrics
        assertThat(metricRegistry.getMetrics()).doesNotContainKey("sparkplug.edgeNode.device1.status");
        assertThat(metricsHolder.getCurrentDeviceOnline().getCount()).isEqualTo(0);
    }

    @Test
    void invalid_protobuf_payload_does_not_throw() {
        when(publishPacket.getTopic()).thenReturn("spBv1.0/group/NBIRTH/edgeNode");
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(new byte[]{0x01, 0x02, 0x03})));

        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);

        // no status metrics should be registered from invalid payload
        assertThat(metricRegistry.getMetrics()).isEmpty();
    }

    private void publishWith(final @NotNull String topic, final @NotNull SparkplugBProto.Payload payload) {
        when(publishPacket.getTopic()).thenReturn(topic);
        when(publishPacket.getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(payload.toByteArray())));
        interceptor.onInboundPublish(publishInboundInput, publishInboundOutput);
    }

    private double statusGaugeValue(final @NotNull String metricName) {
        return ((SettableDoubleGauge) metricRegistry.getMetrics().get(metricName)).getValue();
    }
}
