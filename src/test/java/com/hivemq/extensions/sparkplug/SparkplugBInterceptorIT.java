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

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.testcontainer.core.GradleHiveMQExtensionSupplier;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class SparkplugBInterceptorIT {

    @RegisterExtension
    public final @NotNull HiveMQTestContainerExtension container =
            new HiveMQTestContainerExtension("hivemq/hivemq4", "4.5.2")
                    .withExtension(new GradleHiveMQExtensionSupplier(Paths.get("").toAbsolutePath().toFile()).get())
                    .waitForExtension("HiveMQ Sparkplug Extension")
                    .withLogLevel(Level.TRACE);

    @Ignore
    @Test
    void test_DBIRTH() {
        final String DEATH_TOPIC = "spBv1_0/group1/eon1/NDEATH";
        final String BIRTH_TOPIC = "spBv1_0/group1/eon1/NBIRTH";

        final Mqtt5BlockingClient client = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .buildBlocking();

        final Mqtt5AsyncClient  subscriber = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .buildAsync();

        assertTrue(container.isRunning());

        AtomicBoolean birthMessageReceived= new AtomicBoolean(false);
        AtomicBoolean deathMessageReceived= new AtomicBoolean(false);
        subscriber.connect();
        subscriber.subscribeWith().topicFilter(BIRTH_TOPIC).callback(publish -> {
            birthMessageReceived.set(true);
        });
        subscriber.subscribeWith().topicFilter(DEATH_TOPIC).callback(publish -> {
            deathMessageReceived.set(true);
        });

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

        client.disconnect();
        //assertTrue("Birth message published", birthMessageReceived.get());
        //assertTrue("Death message published", deathMessageReceived.get());

    }
}
