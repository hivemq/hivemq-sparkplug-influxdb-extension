import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.hivemq.extension")
    id("com.google.protobuf")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
    id("org.asciidoctor.jvm.convert")
    idea
}

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Extension - an extension to monitor sparkplug data with influxdata."

hivemqExtension {
    name.set("HiveMQ Sparkplug Extension")
    author.set("HiveMQ")
    priority.set(0)
    startPriority.set(1000)
    mainClass.set("$group.sparkplug.SparkplugExtensionMain")
    sdkVersion.set("${property("hivemq-extension-sdk.version")}")

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:${property("protobuf.version")}")
    implementation("com.izettle:dropwizard-metrics-influxdb:${property("dropwizard-metrics-influxdb.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${property("protobuf.version")}"
    }
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:${property("wiremock.version")}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestImplementation("org.testcontainers:influxdb:${property("testcontainers.version")}")
    integrationTestImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("hivemq-testcontainer.version")}")
    integrationTestImplementation("com.hivemq:hivemq-mqtt-client:${property("hivemq-mqtt-client.version")}")
}

/* ******************** checks ******************** */

license {
    header = projectDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

/* ******************** debugging ******************** */

val unzipHivemq by tasks.registering(Sync::class) {
    from(zipTree(rootDir.resolve("/your/path/to/hivemq-<VERSION>.zip")))
    into({ temporaryDir })
}

tasks.prepareHivemqHome {
    hivemqHomeDirectory.set(layout.dir(unzipHivemq.map { it.destinationDir.resolve("hivemq-<VERSION>") }))
}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(false)
    }
}