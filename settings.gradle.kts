pluginManagement {
    plugins {
        id("com.hivemq.extension") version "1.0.0"
        id("com.google.protobuf") version "0.8.14"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("com.github.sgtsilvio.gradle.utf8") version "${extra["plugin.utf8.version"]}"
        id("org.asciidoctor.jvm.convert") version "${extra["plugin.asciidoctor.version"]}"
    }
}

rootProject.name = "hivemq-sparkplug-extension"

