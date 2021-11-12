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

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparkplugBInterceptorIT {

    @RegisterExtension
    public final @NotNull HiveMQTestContainerExtension container =
            new HiveMQTestContainerExtension(DockerImageName.parse("hivemq/hivemq4").withTag("4.5.3"))
                    .withExtension(MountableFile.forClasspathResource("hivemq-sparkplug-extension"))
                    .waitForExtension("HiveMQ Sparkplug Extension")
                    .withLogLevel(Level.TRACE);

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test_DBIRTH() throws Exception {

        final String DEATH_TOPIC = "spBv1.0/group1/NDEATH/eon1";
        final String BIRTH_TOPIC = "spBv1.0/group1/NBIRTH/eon1";

        final Mqtt5BlockingClient client = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .identifier("EON1")
                .buildBlocking();

        final Mqtt5BlockingClient subscriber = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .identifier("SCADA")
                .buildBlocking();

        subscriber.connect();

        final CompletableFuture<Mqtt5Publish> publishBIRTH = new CompletableFuture<>();
        final CompletableFuture<Mqtt5Publish> publishDEATH = new CompletableFuture<>();
        subscriber.toAsync().subscribeWith().topicFilter(BIRTH_TOPIC).callback(publishBIRTH::complete).send().get();
        subscriber.toAsync().subscribeWith().topicFilter(DEATH_TOPIC).callback(publishDEATH::complete).send().get();

        final Mqtt5Publish will = Mqtt5Publish.builder()
                .topic(DEATH_TOPIC)
                .payload("".getBytes(StandardCharsets.UTF_8))
                .build();
        client.connectWith().willPublish(will).send();

        final Mqtt5Publish birthPublish = Mqtt5Publish.builder()
                .topic(BIRTH_TOPIC)
                .payload("".getBytes(StandardCharsets.UTF_8))
                .build();
        client.publish(birthPublish);

        final Mqtt5Publish birth = publishBIRTH.get();
        assertEquals(BIRTH_TOPIC, birth.getTopic().toString());

        // disconnect triggers a death certificate
        client.disconnectWith().reasonCode(Mqtt5DisconnectReasonCode.DISCONNECT_WITH_WILL_MESSAGE).send();

        final Mqtt5Publish death = publishDEATH.get();
        assertEquals(DEATH_TOPIC, death.getTopic().toString());

        subscriber.disconnect();
    }
}