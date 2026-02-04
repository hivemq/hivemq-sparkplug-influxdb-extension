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

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.domain.InfluxQLQuery;
import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.eclipse.tahu.protobuf.SparkplugBProto;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SuppressWarnings("resource")
@Testcontainers
class SparkplugBInterceptorIT {

    private static final @NotNull String SPARKPLUG_VERSION = "spBv1.0";
    private static final @NotNull String GROUP_ID = "testGroup";
    private static final @NotNull String EDGE_NODE_ID = "testEdgeNode";
    private static final @NotNull String DEVICE_ID = "testDevice";
    private static final @NotNull String INFLUXDB_DATABASE = "hivemq";

    private final @NotNull Network network = Network.newNetwork();

    @Container
    private final @NotNull InfluxDBContainer<?> influxDB =
            new InfluxDBContainer<>(OciImages.getImageName("influxdb")).withAuthEnabled(false)
                    .withNetwork(network)
                    .withNetworkAliases("influxdb");

    @Container
    private final @NotNull HiveMQContainer hivemq =
            new HiveMQContainer(OciImages.getImageName("hivemq/extensions/hivemq-sparkplug-extension")
                    .asCompatibleSubstituteFor("hivemq/hivemq4")) //
                    .withNetwork(network)
                    .withHiveMQConfig(MountableFile.forClasspathResource("config.xml"))
                    .withCopyToContainer(MountableFile.forClasspathResource("config.properties"),
                            "/opt/hivemq/extensions/hivemq-sparkplug-extension/conf/config.properties")
                    .withLogConsumer(outputFrame -> System.out.print("HiveMQ: " + outputFrame.getUtf8String()))
                    .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

    private Mqtt5BlockingClient mqttClient;

    @BeforeEach
    void setUp() {
        try (final var influxDBClient = InfluxDBClientFactory.create(influxDB.getUrl())) {
            final var createDbQuery = new InfluxQLQuery("CREATE DATABASE \"%s\"".formatted(INFLUXDB_DATABASE), "");
            influxDBClient.getInfluxQLQueryApi().query(createDbQuery);
        }

        mqttClient = Mqtt5Client.builder()
                .serverHost(hivemq.getHost())
                .serverPort(hivemq.getMappedPort(1883))
                .identifier("sparkplug-publisher")
                .buildBlocking();
        mqttClient.connect();
    }

    @AfterEach
    void tearDown() {
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        network.close();
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_nbirth_metrics_forwarded_to_influxdb() {
        final var nbirthTopic = SPARKPLUG_VERSION + "/" + GROUP_ID + "/NBIRTH/" + EDGE_NODE_ID;

        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("testMetric").setAlias(1).build())
                .build();

        mqttClient.publishWith().topic(nbirthTopic).payload(payload.toByteArray()).send();

        try (final var influxDBClient = InfluxDBClientFactory.create(influxDB.getUrl())) {
            await().atMost(Duration.ofSeconds(30))
                    .until(() -> getMetricMaxValue(influxDBClient, "sparkplug.testEdgeNode.status") == 1);
            await().atMost(Duration.ofSeconds(30))
                    .until(() -> getMetricMaxCount(influxDBClient, "sparkplug.eons.current.count") == 1);
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    void test_dbirth_metrics_forwarded_to_influxdb() {
        final var dbirthTopic = SPARKPLUG_VERSION + "/" + GROUP_ID + "/DBIRTH/" + EDGE_NODE_ID + "/" + DEVICE_ID;

        final var payload = SparkplugBProto.Payload.newBuilder()
                .addMetrics(SparkplugBProto.Payload.Metric.newBuilder().setName("testMetric").setAlias(1).build())
                .build();

        mqttClient.publishWith().topic(dbirthTopic).payload(payload.toByteArray()).send();

        try (final var influxDBClient = InfluxDBClientFactory.create(influxDB.getUrl())) {
            await().atMost(Duration.ofSeconds(30))
                    .until(() -> getMetricMaxValue(influxDBClient, "sparkplug.testEdgeNode.testDevice.status") == 1);
            await().atMost(Duration.ofSeconds(30))
                    .until(() -> getMetricMaxCount(influxDBClient, "sparkplug.devices.current.count") == 1);
        }
    }

    private static long getMetricMaxValue(final @NotNull InfluxDBClient client, final @NotNull String metric) {
        return getMetricMax(client, metric, "value");
    }

    private static long getMetricMaxCount(final @NotNull InfluxDBClient client, final @NotNull String metric) {
        return getMetricMax(client, metric, "count");
    }

    private static long getMetricMax(
            final @NotNull InfluxDBClient client,
            final @NotNull String metric,
            final @NotNull String field) {
        final var influxQL = String.format("SELECT MAX(%s) FROM \"%s\"", field, metric);
        final var query = new InfluxQLQuery(influxQL, INFLUXDB_DATABASE);
        final var result = client.getInfluxQLQueryApi().query(query);
        long max = 0;
        for (final var queryResult : result.getResults()) {
            for (final var series : queryResult.getSeries()) {
                for (final var record : series.getValues()) {
                    final var value = getValue(record.getValueByKey("max"));
                    if (value > max) {
                        max = value;
                    }
                }
            }
        }
        return max;
    }

    private static long getValue(final @Nullable Object valueField) {
        if (valueField instanceof Number) {
            return ((Number) valueField).longValue();
        } else if (valueField != null) {
            try {
                return (long) Double.parseDouble(valueField.toString());
            } catch (final NumberFormatException ignored) {
            }
        }
        return Long.MIN_VALUE;
    }
}
