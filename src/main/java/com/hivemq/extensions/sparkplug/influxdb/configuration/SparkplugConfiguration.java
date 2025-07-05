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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Reads a property file containing influxdb properties
 * and provides some utility methods for working with {@link Properties}.
 *
 * @author Christoph Schäbel
 * @author Anja Helmbrecht-Schaar
 */
public class SparkplugConfiguration extends PropertiesReader {

    private static final @NotNull String DB = "influxdb.";

    private static final @NotNull String HOST = DB + "host";
    private static final @NotNull String PORT = DB + "port";
    private static final @NotNull String MODE = DB + "mode";
    private static final @NotNull String PROTOCOL = DB + "protocol";
    private static final @NotNull String REPORTING_INTERVAL = DB + "reportingInterval";
    private static final @NotNull String PREFIX = DB + "prefix";
    private static final @NotNull String DATABASE = DB + "database";
    private static final @NotNull String CONNECT_TIMEOUT = DB + "connectTimeout";
    private static final @NotNull String AUTH = DB + "auth";
    private static final @NotNull String TAGS = DB + "tags";
    // InfluxDB Cloud
    private static final @NotNull String BUCKET = DB + "bucket";
    private static final @NotNull String ORGANIZATION = DB + "organization";

    private static final @NotNull String MODE_DEFAULT = "http";
    private static final @NotNull String PROTOCOL_DEFAULT = "http";
    private static final @NotNull String PREFIX_DEFAULT = "";
    private static final @NotNull String DATABASE_DEFAULT = "hivemq";
    private static final int REPORTING_INTERVAL_DEFAULT = 1;
    private static final int CONNECT_TIMEOUT_DEFAULT = 5000;

    private static final @NotNull String SPARKPLUG_VERSION = "sparkplug.version";
    private static final @NotNull String SPARKPLUG_VERSION_DEFAULT = "spBv1.0";

    private static final @NotNull HashMap<String, String> TAGS_DEFAULT = new HashMap<>();

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugConfiguration.class);

    public SparkplugConfiguration(@NotNull final File configFilePath) {
        super(configFilePath);
    }

    /**
     * Check if mandatory properties exist and are valid. Mandatory properties are port and host.
     *
     * @return <b>true</b> if all mandatory properties exist, else <b>false</b>.
     */
    public boolean validateConfiguration() {
        var countError = 0;
        countError += checkMandatoryProperty(HOST);
        countError += checkMandatoryProperty(PORT);
        if (countError != 0) {
            return false;
        }
        // check for valid port value
        final var port = getProperty(PORT);
        try {
            final int intPort = port != null ? Integer.parseInt(port) : -1;
            if (intPort < 0 || intPort > 65535) {
                log.error("Value for mandatory InfluxDB property {} is not in valid port range.", PORT);
                countError++;
            }
        } catch (final Exception e) {
            log.error("Value for mandatory InfluxDB property {} is not a number.", PORT);
            countError++;
        }
        // check if host is still --INFLUX-DB-IP--
        final var host = getProperty(HOST);
        if (host == null || host.equals("--INFLUX-DB-IP--")) {
            countError++;
        }
        return countError == 0;
    }

    /**
     * Check if mandatory property exists.
     *
     * @param property Property to check.
     * @return 0 if property exists, else 1.
     */
    private int checkMandatoryProperty(@NotNull final String property) {
        Objects.requireNonNull(property, "Mandatory property must not be null");
        final var value = getProperty(property);
        if (value == null) {
            log.error("Mandatory property {} is not set.", property);
            return 1;
        }
        return 0;
    }

    public @NotNull String getMode() {
        return validateStringProperty(MODE, MODE_DEFAULT);
    }

    public @Nullable String getHost() {
        return getProperty(HOST);
    }

    public @NotNull String getDatabase() {
        return validateStringProperty(DATABASE, DATABASE_DEFAULT);
    }

    public @Nullable Integer getPort() {
        try {
            return Integer.parseInt(Objects.requireNonNull(getProperty(PORT)));
        } catch (final NumberFormatException e) {
            log.error("Value for {} is not a number", PORT);
            return null;
        }
    }

    public int getReportingInterval() {
        return validateIntProperty(REPORTING_INTERVAL, REPORTING_INTERVAL_DEFAULT, false, false);
    }

    public int getConnectTimeout() {
        return validateIntProperty(CONNECT_TIMEOUT, CONNECT_TIMEOUT_DEFAULT, false, false);
    }

    public @NotNull String getProtocol() {
        final var protocol = getProperty(PROTOCOL);
        if (protocol == null) {
            log.warn("No protocol configured for InfluxDb, using default: {}", PROTOCOL_DEFAULT);
            return PROTOCOL_DEFAULT;
        }
        return protocol;
    }

    public @NotNull String getPrefix() {
        return validateStringProperty(PREFIX, PREFIX_DEFAULT);
    }

    public @Nullable String getAuth() {
        return getProperty(AUTH);
    }

    public @NotNull Map<String, String> getTags() {
        final var tags = getProperty(TAGS);
        if (tags == null) {
            return TAGS_DEFAULT;
        }
        final var split = StringUtils.splitPreserveAllTokens(tags, ";");
        final var tagMap = new HashMap<String, String>();
        for (final String tag : split) {
            final String[] tagPair = StringUtils.split(tag, "=");
            if (tagPair.length != 2 || tagPair[0].isEmpty() || tagPair[1].isEmpty()) {
                log.warn("Invalid tag format {} for InfluxDB", tag);
                continue;
            }
            tagMap.put(tagPair[0], tagPair[1]);
        }
        return tagMap;
    }

    public @Nullable String getBucket() {
        return getProperty(BUCKET);
    }

    public @Nullable String getOrganization() {
        return getProperty(ORGANIZATION);
    }

    @Override
    public @NotNull String getFilename() {
        return "sparkplug.properties";
    }

    /**
     * Fetch property with given <b>key</b>. If the fetched {@link String} is <b>null</b> the <b>defaultValue</b> will
     * be returned.
     *
     * @param key          Key of the property.
     * @param defaultValue Default value as fallback, if property has no value.
     * @return the actual value of the property if it is set, else the <b>defaultValue</b>.
     */
    private String validateStringProperty(@NotNull final String key, @NotNull final String defaultValue) {
        Objects.requireNonNull(key, "Key to fetch property must not be null");
        Objects.requireNonNull(defaultValue, "Default value for property must not be null");
        final var value = getProperty(key);
        if (value == null) {
            if (!defaultValue.isEmpty()) {
                log.warn("No '{}' configured , using default: {}", key, defaultValue);
            }
            return defaultValue;
        }
        return value;
    }

    /**
     * Fetch property with given <b>key</b>.
     * If the fetched {@link String} value is not <b>null</b> convert the value to an int and check validation
     * constraints if given flags are <b>false</b> before returning the value.
     *
     * @param key             Key of the property
     * @param defaultValue    Default value as fallback, if property has no value
     * @param zeroAllowed     use <b>true</b> if property can be zero
     * @param negativeAllowed use <b>true</b> is property can be negative int
     * @return the actual value of the property if it is set and valid, else the <b>defaultValue</b>
     */
    private int validateIntProperty(
            final @NotNull String key,
            final int defaultValue,
            final boolean zeroAllowed,
            final boolean negativeAllowed) {
        Objects.requireNonNull(key, "Key to fetch property must not be null");
        final var value = properties != null ? properties.getProperty(key) : null;
        if (value == null) {
            log.warn("No '{}' configured, using default: {}", key, defaultValue);
            return defaultValue;
        }
        final int valueAsInt;
        try {
            valueAsInt = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            log.warn("Value for the property '{}' is not a number, original value {}. Using default: {}",
                    key,
                    value,
                    defaultValue);
            return defaultValue;
        }
        if (!zeroAllowed && valueAsInt == 0) {
            log.warn("Value for the property '{}' can't be zero. Using default: {}", key, defaultValue);
            return defaultValue;
        }
        if (!negativeAllowed && valueAsInt < 0) {
            log.warn("Value for the property '{}' can't be negative. Using default: {}", key, defaultValue);
            return defaultValue;
        }
        return valueAsInt;
    }

    public @NotNull String getSparkplugVersion() {
        return validateStringProperty(SPARKPLUG_VERSION, SPARKPLUG_VERSION_DEFAULT);
    }
}
