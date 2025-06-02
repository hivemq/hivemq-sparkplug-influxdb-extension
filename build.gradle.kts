plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.defaults)
    alias(libs.plugins.oci)
    alias(libs.plugins.license)
    idea
}

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Extension - an extension to monitor sparkplug data with influxdata."

hivemqExtension {
    name = "HiveMQ Sparkplug Extension"
    author = "HiveMQ"
    sdkVersion = libs.versions.hivemq.extensionSdk

    resources {
        from("LICENSE")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
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

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
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
                implementation(libs.assertj)
                implementation(libs.mockito)
                implementation(libs.wiremock)
                runtimeOnly(libs.logback.classic)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.gradleOci.junitJupiter)
                implementation(libs.hivemq.mqttClient)
                runtimeOnly(libs.logback.classic)
            }
            oci.of(this) {
                imageDependencies {
                    runtime("hivemq:hivemq4:4.40.0").tag("latest")
                }
            }
        }
    }
}

license {
    header = projectDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("org/eclipse/tahu/protobuf/**")
}
