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
package com.hivemq.extensions.sparkplug.influxdb.topics;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopicStructureTest {

    private final @NotNull String version = "spBv1.0";

    @Test
    void test_isValid() {
        final String topic = "spBv1.0/location/NBIRTH/eon1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getMessageType()).isEqualTo(MessageType.NBIRTH);
        assertThat(structure.getEonId()).matches("eon1");
        assertThat(structure.isValid(version)).as("topic structure is valid").isTrue();
    }

    @Test
    void test_isValidDevice() {
        final String topic = "spBv1.0/location/DBIRTH/eon1/dev1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getEonId()).matches("eon1");
        assertThat(structure.getDeviceId()).matches("dev1");
        assertThat(structure.isValid(version)).as("topic structure is valid").isTrue();
    }

    @Test
    void test_isValidMessageType() {
        final String topic = "spBv1.0/location/DBIRTH/eon1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getEonId()).matches("eon1");
        assertThat(structure.getMessageType()).isEqualTo(MessageType.DBIRTH);
        assertThat(structure.isValid(version)).as("topic structure is valid").isTrue();
    }

    @Test
    void test_isNotValidMessageType() {
        final String topic = "spBv1.0/location/ABC/eon1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getEonId()).matches("eon1");
        assertThat(structure.getMessageType()).isEqualTo(MessageType.UNKNOWN);
        assertThat(structure.isValid(version)).as("topic structure is not valid").isFalse();
    }

    @Test
    void test_isNotValidNamespace() {
        final String topic = "spXY/location/DBIRTH/eon1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getNamespace()).matches("spXY");
        assertThat(structure.isValid(version)).as("topic structure is not valid").isFalse();
    }

    @Test
    void test_isNotEnoughTopicLevels() {
        final String topic = "spBv1.0/location/eon1";
        final TopicStructure structure = new TopicStructure(topic);
        assertThat(structure.getMessageType()).isNull();
        assertThat(structure.getEonId()).isNull();
        assertThat(structure.isValid(version)).as("topic structure is not valid").isFalse();
    }
}
