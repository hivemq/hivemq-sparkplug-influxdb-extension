package com.hivemq.extensions.sparkplug;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import com.hivemq.extensions.sparkplug.metrics.MetricsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkplugExtensionMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugExtensionMain.class);
    private MetricsHolder metricsHolder;

    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput, @NotNull ExtensionStartOutput extensionStartOutput) {

        final ExtensionInformation extensionInformation = extensionStartInput.getExtensionInformation();

        try {
            metricsHolder = new MetricsHolder(Services.metricRegistry());
            resetStatusMetrics(metricsHolder);
            addSparkplugMetricsInterceptor();
            log.info("Started " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
        } catch (Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }

    }

    private void resetStatusMetrics(MetricsHolder metricsHolder) {
        //log.debug(" get initial eon count for {}", metricsHolder.getCurrentEonsOnline().getCount() );
        long x = metricsHolder.getCurrentEonsOnline().getCount();
        metricsHolder.getCurrentEonsOnline().dec(x);
        //log.debug(" get initial dev count for {}",metricsHolder.getCurrentDeviceOnline().getCount() );
        long y = metricsHolder.getCurrentDeviceOnline().getCount();
        metricsHolder.getCurrentDeviceOnline().dec(y);
        metricsHolder.getStatusMetrics("scada_0", null).setValue(0);
    }

    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput, @NotNull ExtensionStopOutput extensionStopOutput) {
        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        resetStatusMetrics(metricsHolder);
        log.info("Stopped " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
    }

    private void addSparkplugMetricsInterceptor() {
        final InitializerRegistry initializerRegistry = Services.initializerRegistry();
        final SparkplugBInterceptor sparkplugBInterceptor = new SparkplugBInterceptor(metricsHolder);
        initializerRegistry.setClientInitializer((initializerInput, clientContext) -> {
            clientContext.addPublishInboundInterceptor(sparkplugBInterceptor);
        });
    }

}
