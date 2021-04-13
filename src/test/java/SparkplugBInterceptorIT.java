import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.testcontainer.core.GradleHiveMQExtensionSupplier;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class SparkplugBInterceptorIT {

    @RegisterExtension
    public final @NotNull HiveMQTestContainerExtension container =
            new HiveMQTestContainerExtension("hivemq/hivemq4", "4.5.2")
                    .withExtension(new GradleHiveMQExtensionSupplier(Paths.get("").toAbsolutePath().toFile()).get())
                    .waitForExtension("HiveMQ Sparkplug Extension")
                    .withLogLevel(Level.TRACE);

    @Test
    void test_DBIRTH() {
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
                .serverHost("localhost")
                .serverPort(container.getMqttPort())
                .buildBlocking();

        assertTrue(container.isRunning()); ;

    }
}
