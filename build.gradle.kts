plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    alias(libs.plugins.asciidoctor)
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
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
        from("README.adoc") { rename { "README.txt" } }
        from(tasks.asciidoctor)
    }
}

dependencies {
    implementation(libs.protobuf)
    implementation(libs.dropwizard.metrics.influxdb)
    implementation(libs.commonsLang)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:" + libs.versions.protobuf.get()
    }
}

tasks.asciidoctor {
    sourceDirProperty.set(layout.projectDirectory)
    sources("README.adoc")
    secondarySources { exclude("**") }
}

/* ******************** test ******************** */

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.wiremock.jre8.standalone)
    testRuntimeOnly(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestImplementation(libs.testcontainers.junitJupiter)
    integrationTestImplementation(libs.testcontainers.hivemq)
    integrationTestImplementation(libs.hivemq.mqttClient)
    integrationTestRuntimeOnly(libs.logback.classic)
}

/* ******************** checks ******************** */

license {
    header = projectDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("org/eclipse/tahu/protobuf/**")
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