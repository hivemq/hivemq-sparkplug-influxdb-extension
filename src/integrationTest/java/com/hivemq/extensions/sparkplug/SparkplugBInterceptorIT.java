/*
 * Copyright 2021 HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.testcontainer.core.GradleHiveMQExtensionSupplier;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SparkplugBInterceptorIT {

    @RegisterExtension
    public final @NotNull HiveMQTestContainerExtension container =
            new HiveMQTestContainerExtension("hivemq/hivemq4", "4.5.3")
                    .withExtension(new GradleHiveMQExtensionSupplier(Paths.get("").toAbsolutePath().toFile()).get())
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

        assertTrue(subscriber.getState().isConnected());

        subscriber.toAsync().subscribeWith().topicFilter(BIRTH_TOPIC)
                .callback(publishBIRTH::complete).send().get();

        subscriber.toAsync().subscribeWith().topicFilter(DEATH_TOPIC)
                .callback(publishDEATH::complete).send().get();


        @Nullable Mqtt5Publish will = Mqtt5Publish.builder()
                .topic(DEATH_TOPIC)
                .payload(new String().getBytes(StandardCharsets.UTF_8))
                .build();

        Mqtt5Connect connect = Mqtt5Connect.builder().willPublish(will).build();
        client.connect(connect);

        assertTrue(client.getState().isConnected());

        @Nullable Mqtt5Publish birthPublish = Mqtt5Publish.builder()
                .topic(BIRTH_TOPIC)
                .payload(new String().getBytes(StandardCharsets.UTF_8))
                .build();
        client.publish(birthPublish);

        final Mqtt5Publish birth = publishBIRTH.get();
        assertEquals(BIRTH_TOPIC, birth.getTopic().toString());

        // disconnect triggers a death certificate
        client.disconnect();
        subscriber.disconnect();
        container.close();
    }
}
