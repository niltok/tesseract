import java.util.*
import java.text.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.*

plugins {
    java
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "java")

group = "niltok.tesseract"
version = "Rolling"
val MAIN_CLASS = "niltok.tesseract.AppKt"
val exposedVersion: String by project

repositories {
    // maven("https://mirrors.huaweicloud.com/repository/maven/")
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("net.mamoe:mirai-core:2.12.2")
    implementation("com.github.elbekD:kt-telegram-bot:1.4.1")
    implementation("io.ktor:ktor-server-netty:2.1.2")
    //implementation("dev.inmo:tgbotapi:0.37.1")
    implementation("com.jcabi:jcabi-manifests:1.2.1")
    implementation(fileTree("src/main/resources/libs"))
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("io.github.fanyong920:jvppeteer:1.1.5")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("io.lettuce:lettuce-core:6.2.1.RELEASE")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation("com.squareup:gifencoder:0.10.1")
    testImplementation(group = "junit", name = "junit", version = "4.12")
}

java {
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
    mainClass.set(MAIN_CLASS)
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = MAIN_CLASS
    }
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    manifest {
        attributes["Main-Class"] = MAIN_CLASS
        attributes["Version"] = SimpleDateFormat("yyyy/M/dd HH:mm:ss").format(Date())
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}
