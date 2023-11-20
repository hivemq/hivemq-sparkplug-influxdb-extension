plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    idea
}

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Extension - an extension to monitor sparkplug data with influxdata."

hivemqExtension {
    name.set("HiveMQ Sparkplug Extension")
    author.set("HiveMQ")
    priority.set(0)
    startPriority.set(1000)
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
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

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.mockito)
                implementation(libs.wiremock)
                runtimeOnly(libs.logback.classic)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.hivemq.mqttClient)
                runtimeOnly(libs.logback.classic)
            }
        }
    }
}

license {
    header = projectDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("org/eclipse/tahu/protobuf/**")
}
