plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

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

// Credentials are read by the plugin via `providers.gradleProperty(...)`, which only sees
// gradle properties (NOT `extra[...]`). Set them via env vars prefixed with
// ORG_GRADLE_PROJECT_ when invoking the publish task, e.g.:
//
//   ORG_GRADLE_PROJECT_mavenCentralUsername="$SONATYPE_USERNAME" \
//   ORG_GRADLE_PROJECT_mavenCentralPassword="$SONATYPE_PASSWORD" \
//   ORG_GRADLE_PROJECT_signingInMemoryKey="$SIGNING_KEY" \
//   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$SIGNING_PASSWORD" \
//     ./gradlew :sdk:publishAndReleaseToMavenCentral

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.rimdoo", "anthropic-kotlin", project.version.toString())

    configure(com.vanniktech.maven.publish.KotlinJvm(
        javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
        sourcesJar = true,
    ))

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
