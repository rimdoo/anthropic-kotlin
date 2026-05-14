plugins {
    alias(libs.plugins.kotlin.jvm)
    application
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

application {
    // Default `./gradlew :sample:run` runs the quickstart.
    mainClass.set("com.rimdoo.anthropic.sample.QuickstartKt")
}

tasks.register<JavaExec>("runStreaming") {
    group = "application"
    description = "Stream a haiku from Claude as it's generated"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rimdoo.anthropic.sample.StreamingDemoKt")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runToolUse") {
    group = "application"
    description = "Demonstrate a single tool-use roundtrip"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rimdoo.anthropic.sample.ToolUseDemoKt")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runManagedAgents") {
    group = "application"
    description = "Managed Agents quickstart — create agent + env + session, stream events"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.rimdoo.anthropic.sample.ManagedAgentsQuickstartKt")
    standardInput = System.`in`
}
