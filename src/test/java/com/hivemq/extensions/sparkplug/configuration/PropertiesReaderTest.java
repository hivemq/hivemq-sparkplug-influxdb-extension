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
package com.hivemq.extensions.sparkplug.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import com.hivemq.extensions.sparkplug.configuration.PropertiesReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropertiesReaderTest {

    @TempDir
    public TemporaryFolder folder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void readPropertiesFromFile_file_null() {
        new PropertiesReader(null) {
            @Override
            public String getFilename() {
                return "test";
            }
        };
    }


    @Test
    public void readPropertiesFromFile_file_does_not_exist() {

        final File root = folder.getRoot();

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();

        assertFalse(fileExists);
    }

    @Test
    public void readPropertiesFromFile_file_does_exist() throws IOException {
        final File root = folder.getRoot();

        folder.newFile("test");

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();

        assertTrue(fileExists);
    }

    @Test(expected = NullPointerException.class)
    public void getProperty_key_null() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        propertiesReader.getProperty(null);
    }

    @Test
    public void getProperty_key_doesnt_exist() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);

        final String property1 = propertiesReader.getProperty("unknown");
        assertNull(property1);
    }

    @Test
    public void getProperty_key_exists() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final boolean fileExists = propertiesReader.readPropertiesFromFile();
        assertTrue(fileExists);

        final String property = propertiesReader.getProperty("key");
        assertEquals("value", property);
    }

    @Test
    public void getProperty_before_loading_properties() throws IOException {
        final File root = folder.getRoot();

        final File file = folder.newFile("test");

        final List<String> lines = Collections.singletonList("key:value");
        Files.write(file.toPath(), lines, Charset.forName("UTF-8"));

        final PropertiesReader propertiesReader = new PropertiesReader(root) {
            @Override
            public String getFilename() {
                return "test";
            }
        };

        final String property = propertiesReader.getProperty("key");
        assertNull(property);
    }

}