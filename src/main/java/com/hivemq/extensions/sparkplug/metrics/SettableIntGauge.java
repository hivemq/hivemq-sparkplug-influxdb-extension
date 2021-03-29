package com.hivemq.extensions.sparkplug.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

/**
 * <p>
 * Works like a Gauge, but rather than getting its value from a callback, the value
 * is set when needed.  This can be somewhat convienent, but direct use of a Gauge is likely better
 * </p>
 * <p>
 *     Usage example:
 *     <pre>{@code
 *       MetricRegister metricRegistry;
 *       SettableLongGauge settable = metricRegistry.register("metric.name", new SettableLongGauge());
 *       // ...
 *       settable.setValue(100);
 *       // ...
 *       settable.setValue(200);
 *     }
 *
 *     </pre>
 */
public class SettableIntGauge implements Metric, Gauge<Integer> {
    /**
     * Current value.  Assignment will be atomic.  <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.7">See 17.7</a>
     */
    private volatile int value = 0;

    /**
     * Set the current value the {@link Gauge} will return to something else.
     * @param value    last set value
     * @return itself
     */
    public SettableIntGauge setValue(int value) {
        this.value = value;
        return this;
    }

    /**
     * The last value set by {@link #setValue(int)}}
     * @return Last set value, or zero.
     */
    public Integer getValue() {
        return value;
    }

    public final static class Builder implements MetricBuilder<SettableIntGauge> {
        public static final Builder INSTANCE = new Builder();

        private Builder() {
        }

        @Override
        public SettableIntGauge newMetric() {
            return new SettableIntGauge();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return metric instanceof SettableIntGauge;
        }
    }
}
