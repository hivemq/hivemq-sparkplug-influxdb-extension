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

public class TopicStructure {
    String namespace;
    String groupId;
    String messageType;
    String eonId;
    String scadaId;
    String deviceId;
    boolean valid = false;

    public TopicStructure(@NotNull String topic, String sparkplugVersion) {
        String[] arr = topic.split("/");
        if (arr.length >= 4) {
            namespace = arr[0];
            groupId = arr[1];
            messageType = arr[2];
            if (messageType.matches("STATE")) {
                scadaId = arr[3];
            } else {
                eonId = arr[3];
            }
            if (arr.length > 4) {
                deviceId = arr[4];
            }
            valid = sparkplugVersion.equals(namespace)
                    && groupId != null
                    && messageType != null
                    && (scadaId != null || eonId != null);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getMessageType() {
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

    public boolean isValid() {
        return valid;
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
                ", valid=" + valid +
                '}';
    }
}
