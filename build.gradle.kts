import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("com.hivemq.extension")
    id("com.google.protobuf")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
    id("org.asciidoctor.jvm.convert")
}

/* ******************** metadata ******************** */

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Extension - an extension to monitor sparkplug data with influxdata."

hivemqExtension {
    name = "HiveMQ Sparkplug Extension"
    author = "HiveMQ"
    priority = 0
    startPriority = 1000
    mainClass = "$group.sparkplug.SparkplugExtensionMain"
    sdkVersion = "${property("hivemq-extension-sdk.version")}"
}

/* ******************** dependencies ******************** */

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:${property("protobuf.version")}")
    implementation("com.izettle:dropwizard-metrics-influxdb:${property("dropwizard-metrics-influxdb.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
    implementation("org.jetbrains:annotations:20.1.0")
}

/* ******************** protobuf ******************** */

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.15.8"
    }
}

/* ******************** resources ******************** */

val prepareAsciidoc by tasks.registering(Sync::class) {
    from("README.adoc").into({ temporaryDir })
}

tasks.asciidoctor {
    dependsOn(prepareAsciidoc)
    sourceDir(prepareAsciidoc.map { it.destinationDir })
}

tasks.hivemqExtensionResources {
    from("LICENSE")
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:${property("wiremock.version")}")
    testRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    integrationTestImplementation("org.testcontainers:influxdb:${property("testcontainers.version")}")
    integrationTestImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("hivemq-testcontainer.version")}")
    integrationTestImplementation("com.hivemq:hivemq-mqtt-client:${property("hivemq-mqtt-client.version")}")
}

val integrationTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs integration tests."

    testClassesDirs = sourceSets[name].output.classesDirs
    classpath = sourceSets[name].runtimeClasspath
    shouldRunAfter(tasks.test)
}

tasks.check { dependsOn(integrationTest) }

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
    hivemqFolder.set(unzipHivemq.map { it.destinationDir.resolve("hivemq-<VERSION>") } as Any)
}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(false)
    }
}