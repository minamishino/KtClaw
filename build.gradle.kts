plugins {
    kotlin("jvm") version "2.3.10"
    id("io.ktor.plugin") version "3.4.1"
    application
}

group = "ktclaw"
version = "0.1.0"

repositories {
    maven {
        url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:3.4.1")
    implementation("io.ktor:ktor-server-netty:3.4.1")
    implementation("io.ktor:ktor-server-websockets:3.4.1")
    implementation("io.ktor:ktor-server-html-builder:3.4.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.1")

    // Logging
    implementation("io.ktor:ktor-server-call-logging:3.4.1")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.9.0")

    // Exposed - SQL Framework with R2DBC support
    implementation("org.jetbrains.exposed:exposed-core:1.1.1")
    implementation("org.jetbrains.exposed:exposed-dao:1.1.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.1.1")
    implementation("org.jetbrains.exposed:exposed-java-time:1.1.1")
    implementation("org.jetbrains.exposed:exposed-r2dbc:1.1.1")

    // R2DBC PostgreSQL
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")

    // R2DBC Pool
    implementation("io.r2dbc:r2dbc-pool:1.0.2.RELEASE")

    // Koin - Dependency Injection
    implementation("io.insert-koin:koin-core:4.0.0")
    implementation("io.insert-koin:koin-ktor:4.0.0")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.0")

    // Hoplite - Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.9.0")

    // BouncyCastle - Ed25519 签名算法
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.4.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ktclaw.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}
