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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SparkplugExtensionMainTest {

    private final @NotNull ExtensionStartInput extensionStartInput = mock();
    private final @NotNull ExtensionStartOutput extensionStartOutput = mock();
    private final @NotNull SparkplugExtensionMain main = new SparkplugExtensionMain();

    @TempDir
    private @NotNull Path tempDir;

    @BeforeEach
    void setUp() {
        when(extensionStartInput.getExtensionInformation()).thenReturn(mock());
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(tempDir.toFile());
    }

    @Test
    void extensionStart_whenNoConfigurationFile_thenPreventStartup() {
        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Test
    void extensionStart_whenConfigurationFileNotValid_thenPreventStartup() throws IOException {
        final var confDir = tempDir.resolve("conf");
        Files.createDirectories(confDir);
        Files.write(confDir.resolve("config.properties"), """
                influxdb.host:localhost
                influxdb.port: -3000
                """.lines().toList());

        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Test
    void extensionStart_whenLegacyConfigurationFileNotValid_thenPreventStartup() throws IOException {
        Files.write(tempDir.resolve("sparkplug.properties"), """
                influxdb.host:localhost
                influxdb.port: -3000
                """.lines().toList());

        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }
}
