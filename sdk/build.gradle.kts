plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
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

// Local-only publishing configuration. Maintainer-side; never tracked in git.
// See local/publishing.gradle.kts (gitignored) for the maven-publish + signing setup
// that targets Sonatype / Maven Central. The public build doesn't ship publishing tasks.
val localPublishScript = rootProject.file("local/publishing.gradle.kts")
if (localPublishScript.exists()) {
    apply(from = localPublishScript)
}
