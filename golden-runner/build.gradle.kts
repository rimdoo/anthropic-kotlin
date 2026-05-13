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
    implementation(project(":sdk"))
    implementation(libs.kotlinx.coroutines.core)
}
