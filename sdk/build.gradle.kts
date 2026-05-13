plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.anthropic.java)
    implementation(libs.kotlinx.coroutines.core)
}
