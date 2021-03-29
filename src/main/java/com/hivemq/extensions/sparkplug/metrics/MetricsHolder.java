package com.hivemq.extensions.sparkplug.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.sparkplug.config.SparklplugConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsHolder {
    private final MetricRegistry metricRegistry;
    public  final String METRIC_ROOT= "com.hivemq.sparkplug";

    private static final @NotNull Logger log = LoggerFactory.getLogger(MetricsHolder.class);

    public MetricsHolder(MetricRegistry registry ) {
        metricRegistry = registry;
    }


    public MetricRegistry getMetricRegistry() { return metricRegistry; }

    public SettableDoubleGauge getStatusMetrics(String eonId, String deviceId) {
        String metricName = getMetricName(eonId, deviceId, "status");
        return getSettableDoubleGauge(metricName);
    }

    private String getMetricName(String eonId, String deviceId, String postfix) {
        String metricName = METRIC_ROOT + "." + eonId + ".";
        if   ( deviceId != null ) {
            metricName += deviceId + ".";
        }
        metricName += postfix;
        return metricName;
    }

    public SettableDoubleGauge getDeviceInformationMetricsDouble(String eonId, String deviceId, String information) {
        String metricName = getMetricName(eonId, deviceId, information);
        return getSettableDoubleGauge(metricName);
    }

    public SettableIntGauge getDeviceInformationMetricsInt(String eonId, String deviceId, String information) {
        String metricName = getMetricName(eonId, deviceId, information);
        if( getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableIntGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableIntGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableIntGauge());
    }

    public  SettableLongGauge getDeviceInformationMetricsLong(String eonId, String deviceId, String information) {
        String metricName = getMetricName(eonId, deviceId, information);
        if( getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableLongGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableLongGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableLongGauge());
    }

    public Counter getCurrentDeviceOnline() {
        return getMetricRegistry().counter(METRIC_ROOT + ".devices.current.count");
    }
    public Counter getCurrentEonsOnline() {
        return getMetricRegistry().counter(METRIC_ROOT + ".eons.current.count");
    }

    public SettableDoubleGauge getDeviceDataMetrics(String eonId, String deviceId, String metric) {
        String metricName = getMetricName(eonId, deviceId, metric);
        return getSettableDoubleGauge(metricName);
    }

    private SettableDoubleGauge getSettableDoubleGauge(String metricName) {
        if( getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (SettableDoubleGauge) getMetricRegistry().getMetrics().get(metricName);
        }
        log.debug("Register SettableDoubleGauge metric for: {} ", metricName);
        return getMetricRegistry().register(metricName, new SettableDoubleGauge());

    }
}
