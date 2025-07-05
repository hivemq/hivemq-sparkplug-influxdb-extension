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
package com.hivemq.extensions.sparkplug.influxdb.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metric holder for all generic sparkplug metrics
 *
 * @author Anja Helmbrecht-Schaar
 */

public class MetricsHolder {

    public static final @NotNull String METRIC_ROOT = "sparkplug";

    private static final @NotNull Logger log = LoggerFactory.getLogger(MetricsHolder.class);

    private final @NotNull MetricRegistry metricRegistry;

    public MetricsHolder(final @NotNull MetricRegistry registry) {
        metricRegistry = registry;
    }

    public @NotNull MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public @NotNull SettableDoubleGauge getStatusMetrics(final @NotNull String eonId, final @Nullable String deviceId) {
        final var metricName = getMetricName(eonId, deviceId, "status");
        return getSettableDoubleGauge(metricName);
    }

    private @NotNull String getMetricName(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @Nullable String postfix) {
        var metricName = METRIC_ROOT + "." + eonId + ".";
        if (deviceId != null) {
            metricName += deviceId + ".";
        }
        metricName += postfix;
        return metricName;
    }

    public @NotNull SettableDoubleGauge getDeviceInformationMetricsDouble(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @Nullable String information) {
        final var metricName = getMetricName(eonId, deviceId, information);
        return getSettableDoubleGauge(metricName);
    }

    public @NotNull SettableIntGauge getDeviceInformationMetricsInt(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @Nullable String information) {
        final var metricName = getMetricName(eonId, deviceId, information);
        if (getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableIntGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableIntGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableIntGauge());
    }

    public @NotNull SettableLongGauge getDeviceInformationMetricsLong(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @Nullable String information) {
        final var metricName = getMetricName(eonId, deviceId, information);
        if (getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableLongGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableLongGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableLongGauge());
    }

    public @NotNull SettableBooleanGauge getDeviceInformationMetricsBoolean(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @Nullable String information) {
        final var metricName = getMetricName(eonId, deviceId, information);
        if (getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableBooleanGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableBooleanGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableBooleanGauge());
    }

    public @NotNull Counter getCurrentDeviceOnline() {
        return getMetricRegistry().counter(METRIC_ROOT + ".devices.current.count");
    }

    public @NotNull Counter getCurrentEonsOnline() {
        return getMetricRegistry().counter(METRIC_ROOT + ".eons.current.count");
    }

    public @NotNull SettableDoubleGauge getDeviceDataMetrics(
            final @NotNull String eonId,
            final @Nullable String deviceId,
            final @NotNull String metric) {
        final var metricName = getMetricName(eonId, deviceId, metric);
        return getSettableDoubleGauge(metricName);
    }

    private @NotNull SettableDoubleGauge getSettableDoubleGauge(final @NotNull String metricName) {
        if (getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableDoubleGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableDoubleGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableDoubleGauge());
    }
}
