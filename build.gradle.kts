plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "net.hypedmc.grubby"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

tasks {
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("net.hypedmc.grubby.Grubby")
}