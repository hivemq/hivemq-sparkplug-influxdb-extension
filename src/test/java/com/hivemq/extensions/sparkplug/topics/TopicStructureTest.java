package com.hivemq.extensions.sparkplug.topics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicStructureTest {

    private String version = "spBv1.0";

    @Test
    void test_isValid() {
        String topic = "spBv1.0/location/NBIRTH/eon1";
        TopicStructure structure = new TopicStructure(topic);
        assertTrue(structure.getMessageType() == MessageType.NBIRTH );
        assertTrue(structure.getEonId().matches("eon1"));

        assertTrue(structure.isValid(version), "topic structure is valid");
    }
    @Test
    void test_isValidDevice() {
        String topic = "spBv1.0/location/DBIRTH/eon1/dev1";
        TopicStructure structure = new TopicStructure(topic);
        assertTrue(structure.getEonId().matches("eon1"));
        assertTrue(structure.getDeviceId().matches( "dev1"));
        assertTrue(structure.isValid(version), "topic structure is valid");
    }

    @Test
    void test_isValidMessageType() {
        String topic = "spBv1.0/location/DBIRTH/eon1";
        TopicStructure structure = new TopicStructure(topic);
        assertTrue(structure.getEonId().matches("eon1"));
        assertTrue(structure.getMessageType() == MessageType.DBIRTH);
        assertTrue(structure.isValid(version), "topic structure is not valid");
    }

    @Test
    void test_isNotValidMessageType() {
        String topic = "spBv1.0/location/ABC/eon1";
        TopicStructure structure = new TopicStructure(topic);
        assertTrue(structure.getEonId().matches("eon1"));
        assertTrue(structure.getMessageType() == MessageType.UNKNOWN);
        assertFalse(structure.isValid(version), "topic structure is not valid");
    }

    @Test
    void test_isNotValidNamespace() {
        String topic = "spXY/location/DBIRTH/eon1";
        TopicStructure structure = new TopicStructure(topic);
        assertTrue(structure.getNamespace().matches("spXY"));
        assertFalse(structure.isValid(version), "topic structure is not valid");
    }

    @Test
    void test_isNotEnoughTopicLevels() {
        String topic = "spBv1.0/location/eon1";
        TopicStructure structure = new TopicStructure(topic);
        assertNull(structure.getMessageType());
        assertNull(structure.getEonId());
        assertFalse(structure.isValid(version), "topic structure is not valid");
    }

}