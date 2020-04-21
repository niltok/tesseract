import java.util.*
import java.text.*

plugins {
    java
    kotlin("jvm") version "1.3.60"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "java")

group = "tesseract"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.beust:klaxon:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("net.mamoe:mirai-core-qqandroid:0.37.3")
    compile("com.github.elbekD:kt-telegram-bot:1.2.5")
    compileClasspath("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    implementation("com.jcabi:jcabi-manifests:0.7.5")
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

application {
    mainClassName = "goldimax.tesseract.AppKt"
}

val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "goldimax.tesseract.AppKt"
    }
}

val shadowJar: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar by tasks
shadowJar.apply {
    manifest.attributes.apply {
        put("Main-Class", "goldimax.tesseract.Appkt")
    }
    manifest.attributes["Version"] = SimpleDateFormat("yyyy/M/dd HH:mm:ss").format(Date())
}