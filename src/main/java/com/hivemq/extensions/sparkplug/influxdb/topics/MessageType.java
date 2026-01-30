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
package com.hivemq.extensions.sparkplug.influxdb.topics;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of Sparkplug message types as defined in the Sparkplug specification.
 * <p>
 * Sparkplug defines specific message types for lifecycle management, data transmission,
 * and command execution within an Industrial IoT infrastructure:
 * <ul>
 *     <li><b>BIRTH messages</b> - Announce availability and publish metrics/metadata</li>
 *     <li><b>DEATH messages</b> - Announce disconnection or unavailability</li>
 *     <li><b>DATA messages</b> - Publish metric data updates</li>
 *     <li><b>CMD messages</b> - Receive commands from SCADA host applications</li>
 *     <li><b>STATE messages</b> - SCADA host availability status</li>
 * </ul>
 * <p>
 * Message types are prefixed with:
 * <ul>
 *     <li><b>N</b> - Node (Edge of Network node)</li>
 *     <li><b>D</b> - Device (under an edge node)</li>
 * </ul>
 *
 * @author David Sondermann
 */
public enum MessageType {
    /**
     * THE BIRTH certificate message of a device
     */
    DBIRTH,
    /**
     * THE DEATH certificate message of a device
     */
    DDEATH,
    /**
     * THE BIRTH certificate message of an edge node
     */
    NBIRTH,
    /**
     * THE DEATH certificate message of an edge node
     */
    NDEATH,
    /**
     * THE DATA message from a device
     */
    DDATA,
    /**
     * THE DATA message from an edge node
     */
    NDATA,
    /**
     * THE Command message from an SCADA host for a device
     */
    DCMD,
    /**
     * THE Command message from an SCADA host for an edge node
     */
    NCMD,
    /**
     * THE Command message from an SCADA host
     */
    STATE,
    /**
     * THE UNKNOWN - if something else was used
     */
    UNKNOWN;

    public static @NotNull MessageType fromString(final @NotNull String s) {
        try {
            return valueOf(s);
        } catch (final IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}
