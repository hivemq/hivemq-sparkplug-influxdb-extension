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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SparkplugExtensionMainTest {

    private final @NotNull ExtensionStartInput extensionStartInput = mock();
    private final @NotNull ExtensionStartOutput extensionStartOutput = mock();

    private final @NotNull SparkplugExtensionMain main = new SparkplugExtensionMain();

    private @NotNull Path properties;

    @BeforeEach
    void setUp(final @TempDir @NotNull Path tempDir) {
        when(extensionStartInput.getExtensionInformation()).thenReturn(mock());
        when(extensionStartInput.getExtensionInformation().getExtensionHomeFolder()).thenReturn(tempDir.toFile());

        properties = tempDir.resolve("sparkplug.properties");
    }

    @Test
    void extensionStart_whenNoConfigurationFile_thenPreventStartup() {
        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }

    @Test
    void extensionStart_whenConfigurationFileNotValid_thenPreventStartup() throws IOException {
        Files.write(properties, List.of("influxdb.host:localhost", "influxdb.port: -3000"));

        main.extensionStart(extensionStartInput, extensionStartOutput);
        verify(extensionStartOutput).preventExtensionStartup(anyString());
    }
}
