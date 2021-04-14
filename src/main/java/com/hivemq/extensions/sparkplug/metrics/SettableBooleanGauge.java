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

public class SettableBooleanGauge implements Metric, Gauge<Boolean> {
    private volatile boolean value = false;
    public Boolean getValue() {
        return value;
    }

    public SettableBooleanGauge setValue(boolean value) {
        this.value = value;
        return this;
    }

    public final static class Builder implements MetricBuilder<SettableBooleanGauge> {
        public static final Builder INSTANCE = new Builder();
        private Builder() { }

        @Override
        public SettableBooleanGauge newMetric() {
            return new SettableBooleanGauge();
        }

        @Override
        public boolean isInstance(Metric metric) {
            return metric instanceof SettableBooleanGauge;
        }
    }
}
