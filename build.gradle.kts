import java.util.*
import java.text.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.*

plugins {
    java
    kotlin("jvm") version "1.6.0-RC"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

apply(plugin = "com.github.johnrengelman.shadow")
apply(plugin = "java")

group = "goldimax.tesseract"
version = "1.0-SNAPSHOT"
val MAIN_CLASS = "goldimax.tesseract.AppKt"

repositories {
    // maven("https://mirrors.huaweicloud.com/repository/maven/")
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.beust:klaxon:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("net.mamoe:mirai-core:2.7.1")
    implementation("com.github.elbekD:kt-telegram-bot:1.2.5")
    compileOnly("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    implementation("com.jcabi:jcabi-manifests:0.7.5")
    implementation(fileTree("src/main/resources/libs"))
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.aliyun.openservices", "tablestore", "5.4.0")
    implementation("io.ktor:ktor-server-core:1.4.0")
    implementation("io.ktor:ktor-server-netty:1.4.0")
    implementation("com.googlecode.aviator:aviator:5.2.5")
    implementation("io.github.fanyong920:jvppeteer:1.1.3")
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
    mainClassName = MAIN_CLASS
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
