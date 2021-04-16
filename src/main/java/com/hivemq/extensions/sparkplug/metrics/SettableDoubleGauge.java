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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * Works like a Gauge, but rather than getting its value from a callback, the value
 * is set when needed.  This can be somewhat convienent, but direct use of a Gauge is likely better
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 *       MetricRegister metricRegistry;
 *       SettableDoubleGauge settable = metricRegistry.register("metric.name", new SettableDoubleGauge());
 *       // ...
 *       settable.setValue(1.234);
 *       // ...
 *       settable.setValue(3.156);
 *     }
 *     </pre>
 */
public class SettableDoubleGauge implements Metric, Gauge<Double> {
    /**
     * Current value.  Assignment will be atomic.  <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.7">See 17.7</a>
     */
    private volatile double value;

    /**
     * The last value set by {@link #setValue(double)}}
     *
     * @return Last set value, or zero.
     */
    public @NotNull Double getValue() {
        return value;
    }

    /**
     * Set the current value the {@link Gauge} will return to something else.
     *
     * @param value last set value
     * @return itself
     */
    public @NotNull SettableDoubleGauge setValue(double value) {
        this.value = value;
        return this;
    }

}
