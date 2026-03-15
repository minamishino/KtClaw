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
    
    // Exposed - SQL Framework
    implementation("org.jetbrains.exposed:exposed-core:0.58.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.58.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.58.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.58.0")
    
    // R2DBC PostgreSQL
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    
    // Koin - Dependency Injection
    implementation("io.insert-koin:koin-core:4.0.0")
    implementation("io.insert-koin:koin-ktor:4.0.0")
    implementation("io.insert-koin:koin-logger-slf4j:4.0.0")
    
    // Hoplite - Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.9.0")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.9.0")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:3.4.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.10")
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
