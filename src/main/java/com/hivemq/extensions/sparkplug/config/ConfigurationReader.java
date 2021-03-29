package com.hivemq.extensions.sparkplug.config;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import org.aeonbits.owner.ConfigFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationReader {

    public static final String CONFIG_PATH = "sparkplug-configuration.properties";

    private final  ExtensionInformation extensionInformation;

    public ConfigurationReader(final @NotNull ExtensionInformation extensionInformation) {
        this.extensionInformation = extensionInformation;
    }


    public SparklplugConfiguration readConfiguration() throws IOException {

        final File file = new File(extensionInformation.getExtensionHomeFolder(), CONFIG_PATH);

        if (!file.canRead()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        try(FileInputStream in = new FileInputStream(file)) {
            final Properties properties = new Properties();
            properties.load(in);

            return ConfigFactory.create(SparklplugConfiguration.class, properties);
        } catch (IOException e) {
            throw new IOException("Error while reading configuration file.", e);
        }
    }
}
