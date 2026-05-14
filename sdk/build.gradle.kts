plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.rimdoo"
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
    api(libs.anthropic.java)
    implementation(libs.kotlinx.coroutines.core)
}
