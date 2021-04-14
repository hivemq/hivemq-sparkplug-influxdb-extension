import org.gradle.kotlin.dsl.support.unzipTo

plugins {
    id("com.hivemq.extension")
    id("com.google.protobuf")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions.sparkplug"
description = "HiveMQ Sparkplug Extension - a extension to monitor sparkplug data with influxdata."

hivemqExtension {
    name = "HiveMQ Sparkplug Extension"
    author = "HiveMQ"
    priority = 0
    startPriority = 1000
    mainClass = "$group.SparkplugExtensionMain"
    sdkVersion = "4.5.0"
}

repositories {
    mavenCentral()
}

/* Main dependencies */
dependencies {
    implementation("com.google.protobuf:protobuf-java:${property("protobuf.version")}")
    implementation("com.izettle:dropwizard-metrics-influxdb:${property("dropwizard-metrics-influxdb.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons.version")}")
    implementation("com.google.collections:google-collections:${property("collections.version")}")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

/* Test dependencies */
dependencies {
    testImplementation("com.hivemq:hivemq-mqtt-client:${property("hivemq-mqtt-client.version")}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit-jupiter.version")}")
    testImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("testcontainer.version")}")
    testImplementation("org.testcontainers:influxdb:1.15.1")
    testImplementation( "junit:junit:${property("junit.version")}")
    testImplementation( "org.mockito:mockito-core:1.10.19")
    testImplementation( "org.slf4j:slf4j-api:1.7.25")
    testImplementation( "com.github.tomakehurst:wiremock-jre8-standalone:2.25.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

sourceSets {
    val main by getting { }
    main.java.srcDirs("build/generated/source/proto/main/java")
}


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

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

//preparation and tasks to run & debug Hello World Extension locally

val unzipHivemq by tasks.registering(Sync::class) {
    from(zipTree(rootDir.resolve("/Users/ahelmbre/hivemq/hivemq-4.5.1.zip")))
//    from(zipTree(rootDir.resolve("/your/path/to/hivemq-<VERSION>.zip")))

    into({ temporaryDir })
}

tasks.prepareHivemqHome {
    hivemqFolder.set(unzipHivemq.map { it.destinationDir.resolve("hivemq-4.5.1" ) } as Any)
    from("src/main/resources/sparkplug.properties") { into("extensions/hivemq-sparkplug-extension") }
}

tasks.runHivemqWithExtension {
    debugOptions {
        enabled.set(false)
    }
}

tasks.register("copyExtensionToDockerFolder") {
    dependsOn(tasks.hivemqExtensionZip)
    copy {
        from(zipTree("build/hivemq-extension/hivemq-sparkplug-extension-4.5.3.zip"))
        into("deploy/docker")
    }
}


