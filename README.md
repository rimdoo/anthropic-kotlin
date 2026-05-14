# anthropic-kotlin

Idiomatic Kotlin wrapper around [`com.anthropic:anthropic-java`](https://central.sonatype.com/artifact/com.anthropic/anthropic-java).

[![License](https://img.shields.io/badge/license-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-blueviolet.svg)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-17%2B-orange.svg)](https://adoptium.net)
[![Tests](https://img.shields.io/badge/tests-118%20passing-success.svg)](sdk/src/test/kotlin)

```kotlin
val client = AnthropicOkHttpClient.fromEnv()

val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("Hello, Claude!")),
)
println(response.text)
```

## Why

The official Java SDK is full-featured but verbose from Kotlin: builder chains, `Optional<T>`, `CompletableFuture<T>`, and `Iterator` show up in every public signature. This module wraps it with five design invariants:

| Invariant | Java SDK | anthropic-kotlin |
| --- | --- | --- |
| Async surface | `CompletableFuture<T>` | `suspend fun T` |
| Streams | `StreamResponse<T>` (closeable iterator) | `Flow<T>` |
| Polymorphic types | `Optional<X>` + `isX()` + `asX()` | `sealed class` + `when`-exhaustive |
| Optional fields | `Optional<T>` | Kotlin `T?` |
| Construction | `Foo.builder().bar(...).build()` | named + default parameters |

```kotlin
// Java SDK
MessageCreateParams params = MessageCreateParams.builder()
    .model(Model.CLAUDE_OPUS_4_7)
    .maxTokens(1024)
    .addUserMessage("hi")
    .build();
Message m = client.messages().create(params);
String text = m.content().get(0).asText().text();

// anthropic-kotlin
val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("hi")),
)
val text = response.text
```

## Install

The artifact version tracks the underlying `com.anthropic:anthropic-java` release.

```kotlin
dependencies {
    implementation("io.github.rimdoo:anthropic-kotlin:2.30.0")
}
```

`mavenCentral()` is in your build by default, so no repository changes needed.

## Quickstart

### Set the API key

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

### Single-turn chat

```kotlin
val client = AnthropicOkHttpClient.fromEnv()

val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("What is Kotlin?")),
)
println(response.text)
```

### Multi-turn conversation

```kotlin
client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(
        Message.user("What's Kotlin?"),
        Message.assistant("A JVM language with null-safety and coroutines."),
        Message.user("How is it different from Java?"),
    ),
)
```

### Streaming with `Flow`

```kotlin
client.streamMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("Write a haiku.")),
).collect { event ->
    when (event) {
        is MessageStreamEvent.ContentBlockDelta -> print(event.delta.text)
        is MessageStreamEvent.MessageStop -> println()
        else -> {}
    }
}
```

### System prompt with caching

```kotlin
client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    system = systemPrompt {
        text("Very long static context...", cache = CacheControl.Ephemeral)
        text("Today: ${LocalDate.now()}")
    },
    messages = listOf(Message.user("Question?")),
)
```

### Tool use

```kotlin
val weather = Tool(
    name = "get_weather",
    description = "Look up current weather",
    inputSchema = jsonSchema {
        property("city", type = "string", required = true)
    },
)

val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    tools = listOf(weather),
    toolChoice = ToolChoice.Auto,
    messages = listOf(Message.user("Weather in Seoul?")),
)

val toolUse = response.content.filterIsInstance<ContentBlock.ToolUse>().firstOrNull()
if (toolUse != null) {
    // Run the tool yourself, then send the result back:
    val followup = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        tools = listOf(weather),
        messages = listOf(
            Message.user("Weather in Seoul?"),
            Message.assistant(content = response.content),
            Message.toolResult(toolUseId = toolUse.id, content = "Sunny, 21°C"),
        ),
    )
    println(followup.text)
}
```

### Vision

```kotlin
client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(
        Message.user(
            ContentBlock.Image.url("https://example.com/cat.jpg"),
            ContentBlock.Text("What is this?"),
        ),
    ),
)
```

### Error handling

```kotlin
val result = runCatching {
    client.createMessage(/* ... */)
}
result.onFailure { e ->
    when (e) {
        is AnthropicException.RateLimit -> println("retry after ${e.retryAfter}")
        is AnthropicException.Auth -> error("API key invalid")
        is AnthropicException.BadRequest -> println(e.message)
        else -> throw e
    }
}
```

### Token counting

```kotlin
val tokens = client.countMessageTokens(
    model = Model.CLAUDE_OPUS_4_7,
    messages = listOf(Message.user("Hello, world!")),
)
println(tokens.inputTokens)
```

## Managed Agents

Full coverage of the beta Managed Agents API (Agent / Environment / Session / Vault / MemoryStore + every sub-service). Create an autonomous agent that can run tools inside a managed container, send it user events, and stream events back:

```kotlin
val agent = client.createAgent(
    name = "Coding Assistant",
    model = AgentModel.CLAUDE_OPUS_4_7,
    system = "You write clean, well-documented code.",
    tools = listOf(AgentTool.Toolset20260401), // bash + file ops + web search
)

val environment = client.createEnvironment(name = "quickstart-env")

val session = client.createSession(
    agentId = agent.id,
    environmentId = environment.id,
    title = "Fibonacci task",
)

client.sendSessionEvents(
    sessionId = session.id,
    events = listOf(
        UserEvent.Message(
            text = "Generate the first 20 Fibonacci numbers and save to fibonacci.txt",
        ),
    ),
)

client.streamSessionEvents(session.id)
    .takeWhile { event ->
        when (event) {
            is SessionStreamEvent.AgentMessage -> { print(event.text); true }
            is SessionStreamEvent.AgentToolUse -> { println("\n[Using ${event.name}]"); true }
            is SessionStreamEvent.SessionStatusIdle -> false
            else -> true
        }
    }
    .collect()
```

See [`sample/src/main/kotlin/.../ManagedAgentsQuickstart.kt`](sample/src/main/kotlin/com/rimdoo/anthropic/sample/ManagedAgentsQuickstart.kt) for the runnable version.

## API surface

| Area | Functions |
| --- | --- |
| **Messages** | `createMessage`, `streamMessage`, `countMessageTokens` |
| **Batches** | `createBatch`, `retrieveBatch`, `cancelBatch`, `deleteBatch`, `listBatches`, `batchResults` |
| **Models** | `listModels`, `retrieveModel` |
| **Completions** (legacy) | `createCompletion`, `streamCompletion` |
| **Beta Files** | `uploadFile`, `retrieveFileMetadata`, `listFiles`, `deleteFile` |
| **Managed Agents** | `createAgent` + retrieve/update/archive/list, `listAgentVersions` |
| **Managed Environments** | full CRUD |
| **Managed Sessions** | full CRUD + events (list / send / stream) + threads (list / retrieve / archive / events / stream) + resources (list / retrieve / addFile / update / delete) |
| **Managed Vaults** | full CRUD + credentials (create with `CredentialAuth.{McpOAuth, StaticBearer}` / list / retrieve / archive / delete / mcpOAuthValidate) |
| **Managed MemoryStores** | full CRUD + memories (full CRUD) + memoryVersions (list / retrieve / redact) |

**68 public extension functions** on `AnthropicClient`, every one smoke-tested. See `:sdk:test`.

### Sealed types

`MessageStreamEvent`, `ContentBlock`, `ContentDelta`, `Tool`, `ToolChoice`, `CacheControl`, `ThinkingConfig`, `UserEvent`, `OutcomeRubric`, `CredentialAuth`, `NetworkPolicy`, `AgentTool`, `BatchResultStatus`, `SessionStreamEvent`, `AnthropicException` — all `when`-exhaustive.

### Sample module

```bash
export ANTHROPIC_API_KEY=sk-ant-...

./gradlew :sample:run                 # Quickstart (Messages API)
./gradlew :sample:runStreaming        # Flow<MessageStreamEvent>
./gradlew :sample:runToolUse          # tool-use roundtrip
./gradlew :sample:runManagedAgents    # full Managed Agents flow
```

See [`sample/README.md`](sample/README.md).

## Build & test

```bash
./gradlew :sdk:build                  # compile + assemble
./gradlew :sdk:test                   # 118 unit tests (~3s, no network)
./gradlew :sdk:publishToMavenLocal    # ~/.m2/repository/io/github/rimdoo/...
```

### Test layout

`:sdk:test` runs offline — every test stubs the Java SDK via [mockk](https://mockk.io) so the full Builder chain executes (catching missing-required-field bugs at unit-test time, not at runtime).

| Class | Purpose | Count |
| --- | --- | --- |
| `MappersTest` | Every `*.toRaw()` / `.toParam()` / Message factory / Tool lazy build | 29 |
| `DslTest` | `jsonSchema { }` / `systemPrompt { }` builders | 7 |
| `ExceptionsTest` | `AnthropicException` subtype fields | 7 |
| `ClientApiSmokeTest` | Every public `AnthropicClient.*` extension via mockk-stubbed client | 75 |

## Versioning

`anthropic-kotlin` mirrors the underlying `com.anthropic:anthropic-java` version (currently `2.30.0`). A new wrapper release follows each Java SDK release; only the `gradle/libs.versions.toml` `anthropic` entry needs to change, and the Maven `version` of the artifact tracks it via `libs.versions.anthropic.get()`.

## License

[Apache 2.0](LICENSE) — same as the underlying Java SDK.

## Acknowledgments

Built on top of the official [Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java). This project is not affiliated with Anthropic.
