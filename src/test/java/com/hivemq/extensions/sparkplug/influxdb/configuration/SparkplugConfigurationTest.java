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
package com.hivemq.extensions.sparkplug.influxdb.configuration;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SparkplugConfigurationTest {

    private @NotNull SparkplugConfiguration sparkplugConfiguration;
    private @NotNull Path file;

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() {
        sparkplugConfiguration = new SparkplugConfiguration(tempDir.toFile());
        file = tempDir.resolve("sparkplug.properties");
    }

    @Test
    void validateConfiguration_ok() throws Exception {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:3000"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isTrue();
    }

    @Test
    void validateConfiguration_wrong_port() throws Exception {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:-3000"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_host_missing() throws Exception {
        Files.write(file, List.of("influxdb.port:3000"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_port_missing() throws Exception {
        Files.write(file, List.of("influxdb.host:localhost"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_port_null() throws Exception {
        Files.write(file, List.of("influxdb.host:localhost", "influxdb.port:"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getProperty("port")).isNull();
    }

    @Test
    void validateConfiguration_host_null() throws Exception {
        Files.write(file, List.of("influxdb.host:", "influxdb.port:3000"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getProperty("host")).isNull();
    }

    @Test
    void all_properties_empty() throws Exception {
        Files.write(file, """
                influxdb.mode:
                influxdb.host:
                influxdb.port:
                influxdb.tags:
                influxdb.prefix:
                influxdb.protocol:
                influxdb.database:
                influxdb.connectTimeout:
                influxdb.reportingInterval:
                influxdb.auth:
                """.lines().toList());

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getMode()).isEqualTo("http");
        assertThat(sparkplugConfiguration.getTags()).isEmpty();
        assertThat(sparkplugConfiguration.getPrefix()).isEmpty();
        assertThat(sparkplugConfiguration.getProtocol()).isEqualTo("http");
        assertThat(sparkplugConfiguration.getDatabase()).isEqualTo("hivemq");
        assertThat(sparkplugConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(sparkplugConfiguration.getReportingInterval()).isEqualTo(1);
        assertThat(sparkplugConfiguration.getAuth()).isNull();
        assertThat(sparkplugConfiguration.getHost()).isNull();
    }

    @Test
    void all_properties_null() throws Exception {
        Files.write(file, List.of());

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getMode()).isEqualTo("http");
        assertThat(sparkplugConfiguration.getTags()).isEmpty();
        assertThat(sparkplugConfiguration.getPrefix()).isEmpty();
        assertThat(sparkplugConfiguration.getProtocol()).isEqualTo("http");
        assertThat(sparkplugConfiguration.getDatabase()).isEqualTo("hivemq");
        assertThat(sparkplugConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(sparkplugConfiguration.getReportingInterval()).isEqualTo(1);
        assertThat(sparkplugConfiguration.getAuth()).isNull();
        assertThat(sparkplugConfiguration.getHost()).isNull();
    }

    @Test
    void all_properties_have_correct_values() throws Exception {
        Files.write(file, """
                influxdb.mode:tcp
                influxdb.host:hivemq.monitoring.com
                influxdb.port:3000
                influxdb.tags:host=hivemq1;version=3.4.1
                influxdb.prefix:node1
                influxdb.protocol:tcp
                influxdb.database:test-hivemq
                influxdb.connectTimeout:10000
                influxdb.reportingInterval:5
                influxdb.auth:username:password
                """.lines().toList());

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isTrue();

        assertThat(sparkplugConfiguration.getMode()).isEqualTo("tcp");
        assertThat(sparkplugConfiguration.getTags()).containsExactly( //
                entry("host", "hivemq1"), //
                entry("version", "3.4.1"));
        assertThat(sparkplugConfiguration.getPrefix()).isEqualTo("node1");
        assertThat(sparkplugConfiguration.getProtocol()).isEqualTo("tcp");
        assertThat(sparkplugConfiguration.getDatabase()).isEqualTo("test-hivemq");
        assertThat(sparkplugConfiguration.getConnectTimeout()).isEqualTo(10000);
        assertThat(sparkplugConfiguration.getReportingInterval()).isEqualTo(5);
        assertThat(sparkplugConfiguration.getAuth()).isEqualTo("username:password");
        assertThat(sparkplugConfiguration.getHost()).isEqualTo("hivemq.monitoring.com");
        assertThat(sparkplugConfiguration.getPort()).isEqualTo(3000);
    }

    @Test
    void tags_invalid_configured() throws Exception {
        Files.write(file, List.of("influxdb.tags:host=hivemq1;version=;use=monitoring"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getTags()).containsExactly( //
                entry("use", "monitoring"), //
                entry("host", "hivemq1"));
    }

    @Test
    void tags_has_only_semicolons() throws Exception {
        Files.write(file, List.of("influxdb.tags:;;;;;;"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getTags()).isEmpty();
    }

    @Test
    void tags_has_only_a_key() throws Exception {
        Files.write(file, List.of("influxdb.tags:okay"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getTags()).isEmpty();
    }

    @Test
    void tags_has_correct_tag_but_missing_semicolon() throws Exception {
        Files.write(file, List.of("influxdb.tags:key=value"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        assertThat(sparkplugConfiguration.getTags()).containsExactly(entry("key", "value"));
    }

    @Test
    void properties_that_are_numbers_have_invalid_string() throws Exception {
        Files.write(file, """
                host:test
                influxdb.port:800000
                influxdb.reportingInterval:0
                influxdb.connectTimeout:-1
                """.lines().toList());

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        // false because port is out of range
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();

        // default values because values in file are no valid (zero or negative number)
        assertThat(sparkplugConfiguration.getConnectTimeout()).isEqualTo(5000);
        assertThat(sparkplugConfiguration.getReportingInterval()).isEqualTo(1);
    }

    @Test
    void validateConfiguration_cloud_token_missing() throws Exception {
        Files.write(file, List.of("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.bucket:mybucket"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_cloud_bucket_missing() throws Exception {
        Files.write(file, List.of("influxdb.mode:cloud", "influxdb.host:localhost", "influxdb.token:mytoken"));

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }

    @Test
    void validateConfiguration_cloud_ok() throws Exception {
        Files.write(file, """
                influxdb.mode:cloud
                influxdb.host:localhost
                influxdb.token:mytoken
                influxdb.bucket:mybucket
                """.lines().toList());

        assertThat(sparkplugConfiguration.readPropertiesFromFile()).isTrue();
        assertThat(sparkplugConfiguration.validateConfiguration()).isFalse();
    }
}
