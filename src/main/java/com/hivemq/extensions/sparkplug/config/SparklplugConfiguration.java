package com.hivemq.extensions.sparkplug.config;

import org.aeonbits.owner.Config;

public interface SparklplugConfiguration extends Config {

    /** InfluxDB configuration is set on the influxdb extension **/

    /** Sparkplug Configurations **/

    @DefaultValue("<namespace>/<groupId>/<messageType>/<eonId>/<deviceId>")
    String sparkPlugMetricDeviceState();

    @DefaultValue("<namespace>/<groupId>/<messageType>/<eonId>")
    String sparkPlugTopicEoNState();

    @DefaultValue("<namespace>/<groupId>/STATE/<scadaId>")
    String sparkPlugTopicScadaHost();

}