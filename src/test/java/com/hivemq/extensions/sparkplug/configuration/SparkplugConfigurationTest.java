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
package com.hivemq.extensions.sparkplug.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SparkplugConfigurationTest {

    private @NotNull SparkplugConfiguration sparkplugConfiguration;
    private @NotNull Path file;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        sparkplugConfiguration = new SparkplugConfiguration(tempDir.toFile());
        file = tempDir.resolve("sparkplug.properties");
    }

    @Test
    void validateConfiguration_ok() throws IOException {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:3000"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertTrue(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_wrong_port() throws IOException {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:-3000"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_host_missing() throws IOException {
        Files.write(file, List.of("influxdb.port:3000"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_port_missing() throws IOException {
        Files.write(file, List.of("influxdb.host:localhost"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_port_null() throws IOException {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        assertNull(sparkplugConfiguration.getProperty("port"));
    }

    @Test
    void validateConfiguration_host_null() throws IOException {
        Files.write(file, List.of("influxdb.host:", "influxdb.port:3000"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        assertNull(sparkplugConfiguration.getProperty("host"));
    }

    @Test
    void all_properties_empty() throws IOException {
        Files.write(file, List.of(
                "influxdb.mode:",
                "influxdb.host:",
                "influxdb.port:",
                "influxdb.tags:",
                "influxdb.prefix:",
                "influxdb.protocol:",
                "influxdb.database:",
                "influxdb.connectTimeout:",
                "influxdb.reportingInterval:",
                "influxdb.auth:"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        assertEquals("http", sparkplugConfiguration.getMode());
        assertTrue(sparkplugConfiguration.getTags().isEmpty());
        assertEquals("", sparkplugConfiguration.getPrefix());
        assertEquals("http", sparkplugConfiguration.getProtocol());
        assertEquals("hivemq", sparkplugConfiguration.getDatabase());
        assertEquals(5000, sparkplugConfiguration.getConnectTimeout());
        assertEquals(1, sparkplugConfiguration.getReportingInterval());
        assertNull(sparkplugConfiguration.getAuth());
        assertNull(sparkplugConfiguration.getHost());
    }

    @Test
    void all_properties_null() throws IOException {
        Files.write(file, List.of());

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        assertEquals("http", sparkplugConfiguration.getMode());
        assertTrue(sparkplugConfiguration.getTags().isEmpty());
        assertEquals("", sparkplugConfiguration.getPrefix());
        assertEquals("http", sparkplugConfiguration.getProtocol());
        assertEquals("hivemq", sparkplugConfiguration.getDatabase());
        assertEquals(5000, sparkplugConfiguration.getConnectTimeout());
        assertEquals(1, sparkplugConfiguration.getReportingInterval());
        assertNull(sparkplugConfiguration.getAuth());
        assertNull(sparkplugConfiguration.getHost());
    }

    @Test
    void all_properties_have_correct_values() throws IOException {
        Files.write(file, List.of(
                "influxdb.mode:tcp",
                "influxdb.host:hivemq.monitoring.com",
                "influxdb.port:3000",
                "influxdb.tags:host=hivemq1;version=3.4.1",
                "influxdb.prefix:node1",
                "influxdb.protocol:tcp",
                "influxdb.database:test-hivemq",
                "influxdb.connectTimeout:10000",
                "influxdb.reportingInterval:5",
                "influxdb.auth:username:password"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertTrue(sparkplugConfiguration.validateConfiguration());

        final Map<String, String> tags = sparkplugConfiguration.getTags();
        assertEquals("tcp", sparkplugConfiguration.getMode());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("3.4.1", tags.get("version"));
        assertEquals("node1", sparkplugConfiguration.getPrefix());
        assertEquals("tcp", sparkplugConfiguration.getProtocol());
        assertEquals("test-hivemq", sparkplugConfiguration.getDatabase());
        assertEquals(10000, sparkplugConfiguration.getConnectTimeout());
        assertEquals(5, sparkplugConfiguration.getReportingInterval());
        assertEquals("username:password", sparkplugConfiguration.getAuth());
        assertEquals("hivemq.monitoring.com", sparkplugConfiguration.getHost());
        assertEquals(3000, sparkplugConfiguration.getPort());
    }

    @Test
    void tags_invalid_configured() throws IOException {
        Files.write(file, List.of("influxdb.tags:host=hivemq1;version=;use=monitoring"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        final Map<String, String> tags = sparkplugConfiguration.getTags();
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("monitoring", tags.get("use"));
        assertNull(tags.get("version"));
    }

    @Test
    void tags_has_only_semicolons() throws IOException {
        Files.write(file, List.of("influxdb.tags:;;;;;;"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        final Map<String, String> tags = sparkplugConfiguration.getTags();
        assertEquals(0, tags.size());
    }

    @Test
    void tags_has_only_a_key() throws IOException {
        Files.write(file, List.of("influxdb.tags:okay"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        final Map<String, String> tags = sparkplugConfiguration.getTags();
        assertEquals(0, tags.size());
    }

    @Test
    void tags_has_correct_tag_but_missing_semicolon() throws IOException {
        Files.write(file, List.of("influxdb.tags:key=value"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());

        final Map<String, String> tags = sparkplugConfiguration.getTags();
        assertEquals(1, tags.size());
    }

    @Test
    void properties_that_are_numbers_have_invalid_string() throws IOException {
        Files.write(file, List.of("host:test", "influxdb.port:800000", "influxdb.reportingInterval:0", "influxdb.connectTimeout:-1"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        //false because port is out of range
        assertFalse(sparkplugConfiguration.validateConfiguration());

        //default values because values in file are no valid (zero or negative number)
        assertEquals(5000, sparkplugConfiguration.getConnectTimeout());
        assertEquals(1, sparkplugConfiguration.getReportingInterval());
    }

    @Test
    void validateConfiguration_cloud_token_missing() throws IOException {
        Files.write(file, List.of("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.bucket:mybucket"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_bucket_missing() throws IOException {
        Files.write(file, List.of("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.token:mytoken"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }

    @Test
    void validateConfiguration_cloud_ok() throws IOException {
        Files.write(file, List.of("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.token:mytoken", "influxdb.bucket:mybucket"));

        assertTrue(sparkplugConfiguration.readPropertiesFromFile());
        assertFalse(sparkplugConfiguration.validateConfiguration());
    }
}