rootProject.name = "hivemq-sparkplug-extension"

pluginManagement {
    plugins {
        id("com.hivemq.extension") version "${extra["plugin.hivemq-extension.version"]}"
        id("com.google.protobuf") version "${extra["plugin.protobuf.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("io.github.sgtsilvio.gradle.defaults") version "${extra["plugin.defaults.version"]}"
        id("org.asciidoctor.jvm.convert") version "${extra["plugin.asciidoctor.version"]}"
    }
}