plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
    signing
}

// Maven coordinates:
//   io.github.rimdoo:anthropic-kotlin:<anthropic-java-version>
// The Kotlin package stays com.rimdoo.anthropic (group differs from package on purpose —
// Maven Central requires namespace ownership via DNS or GitHub, and io.github.<user> is
// the recommended path for personal repos that don't own a domain).
group = "io.github.rimdoo"
version = libs.versions.anthropic.get()

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // Both are part of the public API surface:
    //   - anthropic-java: AnthropicOkHttpClient lives here, plus every type our wrappers leak via internal raw
    //   - kotlinx-coroutines-core: Flow<T> appears in public return types
    api(libs.anthropic.java)
    api(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.coroutines.get()}")
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}

// ---------------- Publishing ----------------

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "anthropic-kotlin"
            from(components["java"])

            pom {
                name.set("anthropic-kotlin")
                description.set("Idiomatic Kotlin wrapper around com.anthropic:anthropic-java — suspend / Flow / sealed types over the official Anthropic Java SDK.")
                url.set("https://github.com/rimdoo/anthropic-kotlin")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("rimdoo")
                        name.set("rimdoo")
                        url.set("https://github.com/rimdoo")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/rimdoo/anthropic-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com:rimdoo/anthropic-kotlin.git")
                    url.set("https://github.com/rimdoo/anthropic-kotlin")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/rimdoo/anthropic-kotlin/issues")
                }
            }
        }
    }

    repositories {
        // 1. Local Maven repo (~/.m2/repository) — for testing before publishing publicly.
        //    Trigger: ./gradlew :sdk:publishToMavenLocal
        mavenLocal()

        // 2. GitHub Packages — uses your GitHub PAT (needs `write:packages` scope).
        //    Trigger: ./gradlew :sdk:publishMavenPublicationToGitHubPackagesRepository
        //    Credentials via env:
        //      GITHUB_ACTOR=rimdoo
        //      GITHUB_TOKEN=ghp_...
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rimdoo/anthropic-kotlin")
            credentials {
                username = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
                password = (findProperty("gpr.token") as String?) ?: System.getenv("GITHUB_TOKEN")
            }
        }

        // 3. Sonatype OSSRH (→ Maven Central) — requires:
        //    - https://central.sonatype.com/ account + namespace verification for io.github.rimdoo
        //    - GPG signing key configured below
        //    Trigger: ./gradlew :sdk:publishMavenPublicationToSonatypeRepository
        maven {
            name = "Sonatype"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://central.sonatype.com/repository/maven-snapshots/"
                } else {
                    "https://central.sonatype.com/api/v1/publisher/upload"
                }
            )
            credentials {
                username = (findProperty("sonatype.user") as String?) ?: System.getenv("SONATYPE_USERNAME")
                password = (findProperty("sonatype.token") as String?) ?: System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = (findProperty("signing.key") as String?) ?: System.getenv("SIGNING_KEY")
    val signingPassword = (findProperty("signing.password") as String?) ?: System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}

// Skip signing for local + GitHub Packages — only required for Maven Central.
tasks.withType<Sign>().configureEach {
    onlyIf { gradle.taskGraph.hasTask(":sdk:publishMavenPublicationToSonatypeRepository") }
}
