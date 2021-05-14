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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SparkplugConfigurationTest {

    @TempDir
    File root;

    private File file;

    @BeforeEach
    public void set_up() {
        file = new File(root, "sparkplug.properties");
    }

    @Test
    public void validateConfiguration_ok() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.host:localhost", "influxdb.port:3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertTrue(validateConfiguration);
    }

    @Test
    public void validateConfiguration_wrong_port() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.host:localhost", "influxdb.port:-3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_host_missing() throws IOException {

        final List<String> lines = Collections.singletonList("influxdb.port:3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_port_missing() throws IOException {

        final List<String> lines = Collections.singletonList("influxdb.host:localhost");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_port_null() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.host:localhost", "influxdb.port:");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        assertNull(propertiesReader.getProperty("port"));

        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_host_null() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.host:", "influxdb.port:3000");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        assertNull(propertiesReader.getProperty("host"));

        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void all_properties_empty() throws IOException {

        final List<String> lines = Arrays.asList(
                "influxdb.mode:",
                "influxdb.host:",
                "influxdb.port:",
                "influxdb.tags:",
                "influxdb.prefix:",
                "influxdb.protocol:",
                "influxdb.database:",
                "influxdb.connectTimeout:",
                "influxdb.reportingInterval:",
                "influxdb.auth:");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();


        assertFalse(propertiesReader.validateConfiguration());
        assertEquals("http", propertiesReader.getMode());
        assertTrue(propertiesReader.getTags().isEmpty());
        assertEquals("", propertiesReader.getPrefix());
        assertEquals("http", propertiesReader.getProtocol());
        assertEquals("hivemq", propertiesReader.getDatabase());
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
        assertNull(propertiesReader.getAuth());
        assertNull(propertiesReader.getHost());
        assertNull(propertiesReader.getPort());
    }

    @Test
    public void all_properties_null() throws IOException {

        final List<String> lines = Collections.emptyList();
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();

        assertFalse(propertiesReader.validateConfiguration());
        assertEquals("http", propertiesReader.getMode());
        assertTrue(propertiesReader.getTags().isEmpty());
        assertEquals("", propertiesReader.getPrefix());
        assertEquals("http", propertiesReader.getProtocol());
        assertEquals("hivemq", propertiesReader.getDatabase());
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
        assertNull(propertiesReader.getAuth());
        assertNull(propertiesReader.getHost());
        assertNull(propertiesReader.getPort());
    }

    @Test
    public void all_properties_have_correct_values() throws IOException {

        final List<String> lines = Arrays.asList(
                "influxdb.mode:tcp",
                "influxdb.host:hivemq.monitoring.com",
                "influxdb.port:3000",
                "influxdb.tags:host=hivemq1;version=3.4.1",
                "influxdb.prefix:node1",
                "influxdb.protocol:tcp",
                "influxdb.database:test-hivemq",
                "influxdb.connectTimeout:10000",
                "influxdb.reportingInterval:5",
                "influxdb.auth:username:password");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        @NotNull final Map<String, String> tags = propertiesReader.getTags();

        assertTrue(propertiesReader.validateConfiguration());
        assertEquals("tcp", propertiesReader.getMode());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("3.4.1", tags.get("version"));
        assertEquals("node1", propertiesReader.getPrefix());
        assertEquals("tcp", propertiesReader.getProtocol());
        assertEquals("test-hivemq", propertiesReader.getDatabase());
        assertEquals(10000, propertiesReader.getConnectTimeout());
        assertEquals(5, propertiesReader.getReportingInterval());
        assertEquals("username:password", propertiesReader.getAuth());
        assertEquals("hivemq.monitoring.com", propertiesReader.getHost());
        assertEquals(3000, propertiesReader.getPort());
    }

    @Test
    public void tags_invalid_configured() throws IOException {

        final List<String> lines = Collections.singletonList(
                "influxdb.tags:host=hivemq1;version=;use=monitoring");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertFalse(propertiesReader.validateConfiguration());
        assertEquals(2, tags.size());
        assertEquals("hivemq1", tags.get("host"));
        assertEquals("monitoring", tags.get("use"));
        assertNull(tags.get("version"));
    }

    @Test
    public void tags_has_only_semicolons() throws IOException {

        final List<String> lines = Collections.singletonList(
                "influxdb.tags:;;;;;;");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(0, tags.size());
    }

    @Test
    public void tags_has_only_a_key() throws IOException {

        final List<String> lines = Collections.singletonList(
                "influxdb.tags:okay");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(0, tags.size());
    }

    @Test
    public void tags_has_correct_tag_but_missing_semicolon() throws IOException {

        final List<String> lines = Collections.singletonList(
                "influxdb.tags:key=value");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        final Map<String, String> tags = propertiesReader.getTags();

        assertEquals(1, tags.size());
    }

    @Test
    public void properties_that_are_numbers_have_invalid_string() throws IOException {

        final List<String> lines = Arrays.asList("host:test",
                "influxdb.port:800000", "influxdb.reportingInterval:0", "influxdb.connectTimeout:-1");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);
        propertiesReader.readPropertiesFromFile();

        //false because port is out of range
        assertFalse(propertiesReader.validateConfiguration());

        //default values because values in file are no valid (zero or negative number)
        assertEquals(5000, propertiesReader.getConnectTimeout());
        assertEquals(1, propertiesReader.getReportingInterval());
    }

    @Test
    public void validateConfiguration_cloud_token_missing() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.bucket:mybucket");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_cloud_bucket_missing() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.token:mytoken");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

    @Test
    public void validateConfiguration_cloud_ok() throws IOException {

        final List<String> lines = Arrays.asList("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.token:mytoken", "influxdb.bucket:mybucket");
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        final SparkplugConfiguration propertiesReader = new SparkplugConfiguration(root);

        propertiesReader.readPropertiesFromFile();
        final boolean validateConfiguration = propertiesReader.validateConfiguration();

        assertFalse(validateConfiguration);
    }

}
