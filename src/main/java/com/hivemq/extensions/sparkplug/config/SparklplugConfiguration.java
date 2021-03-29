package com.hivemq.extensions.sparkplug.config;

import org.aeonbits.owner.Config;

public interface SparklplugConfiguration extends Config {

    /** InfluxDB configuration is set on the influxdb extension **/

    /** Sparkplug Configurations **/

    @DefaultValue("spBv1_0/location1/DBIRTH/eon_1/device_1")
    String sparkplugTopicDBIRTH();

    @DefaultValue("spBv1_0/location1/DDATA/eon_1/device_1")
    String sparkplugTopicDDATA();

    @DefaultValue("sparkplug.demo.temperature.value")
    String sparkplugMetricTemp();

    @DefaultValue("sparkplug.demo.sensor.state")
    String sparkplugMetricDoor();

    @DefaultValue("sparkplug.demo.level.value")
    String sparkplugMetricRatTank();

    @DefaultValue("sparkplug.demo.stacklight.state")
    String sparkplugMetricLight();

    @DefaultValue("<namespace>/<groupId>/<messageType>/<eonId>/<deviceId>")
    String sparkPlugMetricDeviceState();

    @DefaultValue("<namespace>/<groupId>/<messageType>/<eonId>")
    String sparkPlugTopicEoNState();

    @DefaultValue("<namespace>/<groupId>/STATE/<scadaId>")
    String sparkPlugTopicScadaHost();

}