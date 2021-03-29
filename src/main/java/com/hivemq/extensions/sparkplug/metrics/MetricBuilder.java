package com.hivemq.extensions.sparkplug.metrics;

import com.codahale.metrics.*;

/**
 * A copy of the MetricBuilder defined inside {@link com.codahale.metrics.MetricRegistry}, but a
 * public version.
 * @param <T>    Which metric type this builds
 */
public interface MetricBuilder<T extends Metric> {
    public T newMetric();

    public boolean isInstance(Metric metric);

    public MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
        @Override
        public Counter newMetric() {
            return new Counter();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return Counter.class.isInstance(metric);
        }
    };

    public MetricBuilder<Histogram> HISTOGRAMS = new MetricBuilder<Histogram>() {
        @Override
        public Histogram newMetric() {
            return new Histogram(new ExponentiallyDecayingReservoir());
        }

        @Override
        public boolean isInstance(Metric metric) {
            return Histogram.class.isInstance(metric);
        }
    };

    public MetricBuilder<Meter> METERS = new MetricBuilder<Meter>() {
        @Override
        public Meter newMetric() {
            return new Meter();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return Meter.class.isInstance(metric);
        }
    };

    public MetricBuilder<Timer> TIMERS = new MetricBuilder<Timer>() {
        @Override
        public Timer newMetric() {
            return new Timer();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return Timer.class.isInstance(metric);
        }
    };
}
