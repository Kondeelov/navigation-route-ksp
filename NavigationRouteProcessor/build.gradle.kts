import java.util.Properties

plugins {
    `maven-publish`
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlin.symbol.processing.api)
    implementation(libs.kotlin.stdlib)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kondeelov/navigation-route-ksp")
            credentials {
                val properties = Properties()
                properties.load(rootProject.file("signing.properties").inputStream())
                username = properties.getProperty("github_packages_username")
                password = properties.getProperty("github_packages_password")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "com.kondee.navigationrouteprocessor"
            artifactId = "navigation-route-ksp"
            version = "0.0.2"
        }
    }
}