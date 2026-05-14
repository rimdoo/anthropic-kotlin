# Changelog

All notable changes to `anthropic-kotlin` are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). The artifact
version mirrors the underlying [`com.anthropic:anthropic-java`](https://central.sonatype.com/artifact/com.anthropic/anthropic-java)
release.

## [Unreleased]

## [2.30.0] — 2026-05-14

First public release, matching `com.anthropic:anthropic-java:2.30.0`.

### Added — Public API (68 extensions on `AnthropicClient`)

- **Messages** — `createMessage` (two overloads: `system: String?` and `system: SystemPrompt`),
  `streamMessage` (two overloads, returns `Flow<MessageStreamEvent>`), `countMessageTokens`.
- **Batches** — `createBatch`, `retrieveBatch`, `cancelBatch`, `deleteBatch`, `listBatches`,
  `batchResults` (`Flow<BatchResult>`). `BatchResultStatus` sealed (`Succeeded` / `Errored` /
  `Canceled` / `Expired`).
- **Models** — `listModels` (`Flow<ModelInfo>`), `retrieveModel`.
- **Completions** (legacy) — `createCompletion`, `streamCompletion`.
- **Beta Files** — `uploadFile`, `retrieveFileMetadata`, `listFiles`, `deleteFile`.
- **Managed Agents** — `createAgent` (typed `AgentModel` + sealed `AgentTool`), `retrieveAgent`,
  `updateAgent`, `archiveAgent`, `listAgents`, `listAgentVersions`.
- **Managed Environments** — full CRUD with `NetworkPolicy.Unrestricted` default.
- **Managed Sessions** — full CRUD plus sub-services:
  - `events` — `listSessionEvents`, `sendSessionEvents` (sealed `UserEvent`: `Message` /
    `Interrupt` / `CustomToolResult` / `ToolConfirmation` / `DefineOutcome` with `OutcomeRubric`),
    `streamSessionEvents` (sealed `SessionStreamEvent`).
  - `threads` — `listSessionThreads`, `retrieveSessionThread`, `archiveSessionThread`,
    `listSessionThreadEvents`, `streamSessionThreadEvents`.
  - `resources` — `listSessionResources`, `retrieveSessionResource` (`SessionResourceDetail`),
    `addSessionFileResource`, `updateSessionResource`, `deleteSessionResource`.
- **Managed Vaults** — full CRUD plus credentials sub-service (`createVaultCredential` with sealed
  `CredentialAuth` of `McpOAuth` / `StaticBearer`, `validateVaultCredentialMcpOAuth`, list /
  retrieve / archive / delete).
- **Managed MemoryStores** — full CRUD plus `memories` (full CRUD) and `memoryVersions` (list /
  retrieve / redact).

### Added — DSLs

- `jsonSchema { property(name, type, required, description) }` — tool input schema builder.
- `systemPrompt { text(content, cache = CacheControl.Ephemeral?) }` — multi-block system prompt
  with prompt-caching control.

### Added — Sealed type hierarchies

- `MessageStreamEvent` — 6 variants matching `RawMessageStreamEvent`.
- `ContentBlock` — `Text` / `ToolUse` / `Thinking` / `Image` / `Other`.
- `ContentDelta` — `Text` / `InputJson` / `Thinking` / `Signature` / `Citations`.
- `ToolChoice` — `Auto` / `Any` / `None` / `Tool(name)`.
- `CacheControl` — `Ephemeral`.
- `ThinkingConfig` — `Enabled(budgetTokens)` / `Disabled`.
- `AnthropicException` — `RateLimit(retryAfter, statusCode)` / `Auth` / `BadRequest` /
  `NotFound` / `Unprocessable` / `Server` / `Unknown`. Mapped at the SDK boundary from the
  Java SDK's `com.anthropic.errors.*` hierarchy.

### Design invariants

- All I/O is `suspend fun` or returns `Flow<T>`. `CompletableFuture<T>` does not appear in any
  public signature.
- Java SDK `Optional<T>` mapped to Kotlin `T?` at the wrapper boundary.
- Java SDK builder types never appear in public signatures.
- Polymorphic Java unions become Kotlin `sealed class` — `when` is exhaustive.
- A single Java interop facade: all top-level functions emit into `AnthropicKt` via
  `@file:JvmName("AnthropicKt") + @file:JvmMultifileClass`.

### Tests

- 118 unit tests, ~3s, fully offline. `:sdk:test` covers:
  - `MappersTest` — every `*.toRaw()` / `.toParam()` / Message factory / Tool lazy build (29).
  - `DslTest` — `jsonSchema { }` and `systemPrompt { }` (7).
  - `ExceptionsTest` — every `AnthropicException` subtype (7).
  - `ClientApiSmokeTest` — every public `AnthropicClient.*` extension via mockk-stubbed
    client; catches builder-time discriminator-field bugs that compile-only checks miss (75).

### Sample

Runnable demos in `:sample`:

- `Quickstart.kt` — mirrors <https://platform.claude.com/docs/en/get-started>.
- `StreamingDemo.kt` — `streamMessage` Flow.
- `ToolUseDemo.kt` — two-round tool-use roundtrip.
- `ManagedAgentsQuickstart.kt` — mirrors <https://platform.claude.com/docs/en/managed-agents/quickstart>.

Run with `./gradlew :sample:run` / `:sample:runStreaming` / `:sample:runToolUse` /
`:sample:runManagedAgents`.

[Unreleased]: https://github.com/rimdoo/anthropic-kotlin/compare/v2.30.0...HEAD
[2.30.0]: https://github.com/rimdoo/anthropic-kotlin/releases/tag/v2.30.0
