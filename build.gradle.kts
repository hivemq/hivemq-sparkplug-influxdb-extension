import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.compileJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.protobuf)
    implementation(libs.metrics.influxdb) {
        // kafka-clients (and its transitive snappy-java, lz4-java) has critical CVEs and is not used by this extension
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }
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
    imageMapping {
        mapModule("com.hivemq", "hivemq-enterprise") {
            toImage("hivemq/hivemq4")
        }
    }
    imageDefinitions.register("main") {
        allPlatforms {
            dependencies {
                runtime("com.hivemq:hivemq-enterprise:latest") { isChanging = true }
            }
            layer("main") {
                contents {
                    permissions("opt/hivemq/", 0b111_111_101)
                    permissions("opt/hivemq/extensions/", 0b111_111_101)
                    into("opt/hivemq/extensions") {
                        from(zipTree(tasks.hivemqExtensionZip.flatMap { it.archiveFile }))
                    }
                }
            }
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
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.mockito)
                implementation(libs.wiremock)
                implementation(libs.logback.classic)
            }
            targets.configureEach {
                testTask {
                    testLogging {
                        events = setOf(
                            TestLogEvent.STARTED,
                            TestLogEvent.PASSED,
                            TestLogEvent.SKIPPED,
                            TestLogEvent.FAILED,
                            TestLogEvent.STANDARD_ERROR,
                        )
                        exceptionFormat = TestExceptionFormat.FULL
                        showStandardStreams = true
                    }
                    reports {
                        junitXml.isOutputPerTestCase = true
                    }
                }
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(project())
                implementation(libs.assertj)
                implementation(libs.awaitility)
                implementation(libs.hivemq.mqttClient)
                implementation(libs.influxdb.client)
                implementation(libs.protobuf)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.testcontainers.influxdb)
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.gradleOci.junitJupiter)
                runtimeOnly(libs.logback.classic)
            }
            oci.of(this) {
                imageDependencies {
                    runtime(project).tag("latest")
                    runtime("library:influxdb:1.4.3").name("influxdb").tag("latest")
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

// configure reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    // normalize file permissions for reproducibility
    // files: 0644 (rw-r--r--), directories: 0755 (rwxr-xr-x)
    filePermissions {
        unix("0644")
    }
    dirPermissions {
        unix("0755")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure consistent compilation across different JDK versions
    options.compilerArgs.addAll(listOf(
        // include parameter names for reflection (improves consistency)
        "-parameters"
    ))
}
