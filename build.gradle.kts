import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("maven-publish")
}

apply {
    plugin("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
    compileOnly("org.jetbrains.dokka:dokka-core:1.6.21")
    implementation("org.jetbrains.dokka:dokka-base:1.6.21")
    implementation("org.jetbrains.dokka:gfm-plugin:1.6.21")
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "gg.pragma"
            artifactId = "dokka-hugo-plugin"
            version = "3.0"

            from(components["kotlin"])
        }
    }
}
