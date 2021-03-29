import org.gradle.kotlin.dsl.support.unzipTo

plugins {
    id("com.hivemq.extension")
    id("com.google.protobuf")
}

group = "com.hivemq"
version = "1.0.0"

repositories {
    mavenCentral()
}
/* Main dependencies */

dependencies {
    implementation("com.google.protobuf:protobuf-java:${property("protobuf.version")}")
    implementation("org.aeonbits.owner:owner-java8:${property("owner.version")}")
    implementation("com.izettle:dropwizard-metrics-influxdb:${property("dropwizard-metrics-influxdb.version")}")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

/* Test dependencies */

dependencies {
    testImplementation("com.hivemq:hivemq-mqtt-client:1.2.1")
    testImplementation("org.testcontainers:influxdb:1.15.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit-jupiter.version")}")
    testImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("testcontainer.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

sourceSets {
    val main by getting { }
    main.java.srcDirs("build/generated/source/proto/main/java")
}

hivemqExtension {
    name = "HiveMQ Sparkplug Extension"
    author = "HiveMQ"
    priority = 0
    startPriority = 1000
    mainClass = "com.hivemq.extensions.sparkplug.SparkplugExtensionMain"
    sdkVersion = "4.5.0"
}

/**
tasks.register("copyExtensionToDockerFolder") {
    dependsOn(tasks.hivemqExtensionZip)
    copy {
        from(zipTree("build/hivemq-extension/hivemq-sparkplug-extension-1.0.0.zip"))
        into("deploy/docker")
    }
}

**/

tasks.prepareHivemqHome {
    hivemqFolder.set("/Users/ahelmbre/hivemq/hivemq-4.5.1")
    //hivemqFolder.set(unzipHivemq.map { it.destinationDir.resolve("hivemq-4.5.1" ) } as Any)
    //from("src/main/resources/configuration.properties") { into("extensions/myEnterpriseExample") }

}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(false)
    }
}
