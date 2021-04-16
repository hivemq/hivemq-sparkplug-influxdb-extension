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
package com.hivemq.extensions.sparkplug.topics;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * Topic structure meta object to create sparkplug structure and
 * validate the incoming topic parts against sparkplug topic specification
 *
 * @author Anja Helmbrecht-Schaar
 */
public class TopicStructure {
    private String namespace;
    private String groupId;
    private MessageType messageType;
    private String eonId;
    private  String scadaId;
    private  String deviceId;

    public TopicStructure(final @NotNull String topic) {
        final String[] arr = topic.split("/");
        if (arr.length >= 4) {
            namespace = arr[0];
            groupId = arr[1];
            messageType = MessageType.fromString(arr[2]);
            if (MessageType.STATE == messageType) {
                scadaId = arr[3];
            } else {
                eonId = arr[3];
            }
            if (arr.length > 4) {
                deviceId = arr[4];
            }
        }
    }

    private boolean isValidNamespace(String sparkplugVersion) {
        return (namespace != null && sparkplugVersion.matches(namespace));
    }

    private boolean isValidMessageType() {
        return ( messageType != MessageType.UNKNOWN);
    }

    public String getNamespace() {
        return namespace;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getEonId() {
        return eonId;
    }

    public String getScadaId() {
        return scadaId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isValid(final @NotNull String sparkplugVersion) {
        return isValidNamespace(sparkplugVersion)
                && isValidMessageType()
                && (scadaId != null || eonId != null);
    }

    @Override
    public String toString() {
        return "TopicStructure{" +
                "namespace='" + namespace + '\'' +
                ", groupId='" + groupId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", eonId='" + eonId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", scadaId='" + scadaId + '\'' +
                '}';
    }
}
