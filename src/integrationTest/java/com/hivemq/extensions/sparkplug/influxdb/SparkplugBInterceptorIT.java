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

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import io.github.sgtsilvio.gradle.oci.junit.jupiter.OciImages;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class SparkplugBInterceptorIT {

    @Container
    final @NotNull HiveMQContainer container = new HiveMQContainer(OciImages.getImageName("hivemq/hivemq4")) //
            .withExtension(MountableFile.forClasspathResource("hivemq-sparkplug-extension"))
            .waitForExtension("HiveMQ Sparkplug Extension")
            .withLogLevel(Level.TRACE)
            .withEnv("HIVEMQ_DISABLE_STATISTICS", "true");

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test_DBIRTH() throws Exception {
        final var deathTopic = "spBv1.0/group1/NDEATH/eon1";
        final var birthTopic = "spBv1.0/group1/NBIRTH/eon1";

        final var client = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .identifier("EON1")
                .buildBlocking();

        final var subscriber = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .identifier("SCADA")
                .buildBlocking();

        subscriber.connect();

        final var publishBIRTH = new CompletableFuture<Mqtt5Publish>();
        final var publishDEATH = new CompletableFuture<Mqtt5Publish>();
        subscriber.toAsync().subscribeWith().topicFilter(birthTopic).callback(publishBIRTH::complete).send().get();
        subscriber.toAsync().subscribeWith().topicFilter(deathTopic).callback(publishDEATH::complete).send().get();

        final var will = Mqtt5Publish.builder()
                .topic(deathTopic)
                .payload("".getBytes(StandardCharsets.UTF_8))
                .build();
        client.connectWith().willPublish(will).send();

        final var birthPublish = Mqtt5Publish.builder()
                .topic(birthTopic)
                .payload("".getBytes(StandardCharsets.UTF_8))
                .build();
        client.publish(birthPublish);

        final var birth = publishBIRTH.get();
        assertEquals(birthTopic, birth.getTopic().toString());

        // disconnect triggers a death certificate
        client.disconnectWith().reasonCode(Mqtt5DisconnectReasonCode.DISCONNECT_WITH_WILL_MESSAGE).send();

        final var death = publishDEATH.get();
        assertEquals(deathTopic, death.getTopic().toString());

        subscriber.disconnect();
    }
}
