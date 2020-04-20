plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "jp.sawa-kai"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")

    compileOnly("org.spigotmc:spigot-api:1.15.2-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")

    implementation("io.socket:socket.io-client:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.5.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.5.0")
    implementation("com.squareup.moshi:moshi:1.9.2")
    implementation("com.squareup.moshi:moshi-kotlin:1.9.2")
    implementation("org.json:json:20190722")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}