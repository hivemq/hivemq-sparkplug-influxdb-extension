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
package com.hivemq.extensions.sparkplug.metrics;

import com.codahale.metrics.*;

/**
 * A copy of the MetricBuilder defined inside {@link com.codahale.metrics.MetricRegistry}, but a
 * public version.
 *
 * @param <T> Which metric type this builds
 */
public interface MetricBuilder<T extends Metric> {
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

    public T newMetric();

    public boolean isInstance(Metric metric);
}
