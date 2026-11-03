import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.PublishingExtension

plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.kover)
    alias(libs.plugins.vanniktech)
    alias(libs.plugins.dokka)
}

// Local maven repository to publish artifacts to
val repoDirectory by extra { "${project.rootDir}/repo" }

dependencies {
    kover(project(":mqtt-core"))
    kover(project(":mqtt-client"))
    kover(project(":mqtt-client-ws"))
}

// Run for example with ./gradlew koverHtmlReport
kover {
    reports {
        filters {
            excludes {
                classes(
                    "de.kempmobil.ktor.mqtt.ClientSample",
                    "de.kempmobil.ktor.mqtt.ws.WsSample"
                )
            }
        }
    }
}

allprojects {
    plugins.withId("com.vanniktech.maven.publish.base") {
        val ghRepo = providers.environmentVariable("GITHUB_REPOSITORY").orNull ?: "ktKongTong/ktor-mqtt"
        configure<MavenPublishBaseExtension> {
            pom {
                name = "Ktor MQTT"
                description = "A modern multiplatform Kotlin MQTT 5 client library."
                inceptionYear = "2024"
                url = "https://github.com/$ghRepo/"
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("ktKongTong")
                        name.set("ktKongTong")
                        url.set("https://github.com/ktKongTong/")
                    }
                }
                scm {
                    url.set("https://github.com/$ghRepo/")
                    connection.set("scm:git:git://github.com/$ghRepo.git")
                    developerConnection.set("scm:git:ssh://git@github.com/$ghRepo.git")
                }
            }
        }

        val ghUser = providers.environmentVariable("GITHUB_ACTOR").orNull
            ?: providers.gradleProperty("gpr.user").orNull
        val ghToken = providers.environmentVariable("GITHUB_TOKEN").orNull
            ?: providers.gradleProperty("gpr.key").orNull
        if (ghUser != null && ghToken != null) {
            configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/$ghRepo")
                        credentials {
                            username = ghUser
                            password = ghToken
                        }
                    }
                }
            }
        }
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
