# anthropic-kotlin

Idiomatic Kotlin wrapper around [`com.anthropic:anthropic-java`](https://central.sonatype.com/artifact/com.anthropic/anthropic-java).

```kotlin
val client = AnthropicOkHttpClient.fromEnv()
val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("Hello, Claude!")),
)
println(response.text)
```

* `suspend fun` everywhere — no `CompletableFuture` in any public signature
* `Flow<T>` for streams — no callbacks, no iterators leaking out
* `sealed` Kotlin types over polymorphic Java unions — `when`-exhaustive
* Named + default parameters — no Java builder chains in your call sites
* Full Managed Agents surface (Agent / Environment / Session / Vault / MemoryStore)
* Versioned to track `anthropic-java` (current: `2.30.0`)

## Install

### Maven Central (once published)

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.rimdoo:anthropic-kotlin:2.30.0")
}
```

### JitPack (no auth)

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.rimdoo:anthropic-kotlin:2.30.0")
}
```

### GitHub Packages

```kotlin
repositories {
    maven("https://maven.pkg.github.com/rimdoo/anthropic-kotlin") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")  // needs read:packages scope
        }
    }
}
dependencies {
    implementation("io.github.rimdoo:anthropic-kotlin:2.30.0")
}
```

## Quickstarts

`./gradlew :sample:run` runs `Quickstart.kt` (Messages API).
`./gradlew :sample:runManagedAgents` runs `ManagedAgentsQuickstart.kt`.
See [`sample/README.md`](sample/README.md) for all entry points.

## Publishing this SDK

```bash
# 1. Local — fast feedback while developing consumers locally
./gradlew :sdk:publishToMavenLocal

# 2. GitHub Packages
GITHUB_ACTOR=rimdoo \
GITHUB_TOKEN=ghp_... \
  ./gradlew :sdk:publishMavenPublicationToGitHubPackagesRepository

# 3. Maven Central (Sonatype Central Portal)
SONATYPE_USERNAME=... \
SONATYPE_PASSWORD=... \
SIGNING_KEY="$(cat ~/.gnupg/secring.gpg.asc)" \
SIGNING_PASSWORD=... \
  ./gradlew :sdk:publishMavenPublicationToSonatypeRepository
```

Pre-flight checklist for Maven Central:

* Register at https://central.sonatype.com/ and verify the `io.github.rimdoo` namespace.
* Generate a GPG key, publish to `keys.openpgp.org`, export the secret key as ASCII-armored.
* Tag a release in git (`git tag v2.30.0 && git push --tags`) so JitPack can pick it up.

## License

[Apache 2.0](LICENSE)
