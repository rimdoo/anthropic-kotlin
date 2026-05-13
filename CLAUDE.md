# sendbird-anthropic-kotlin

Kotlin wrapper around `com.anthropic:anthropic-java:2.30.0`.

## Goal

Replace the Java SDK's ergonomics with idiomatic Kotlin:

- **No builder chains.** Public functions use `default + named parameters`.
- **`suspend fun` everywhere** for async. `CompletableFuture<T>` / blocking calls → `suspend fun T`.
- **`Flow<MessageStreamEvent>` for streams.** No iterators, channels, or callbacks in public API.
- **Sealed hierarchies** for polymorphic Java types (`ContentBlock`, `StopReason`, ...).
- **No leaky Java types.** No `Optional<T>`, `CompletableFuture<T>`, or Java builder classes in public signatures.

## Workflow

Three custom agents under `.claude/agents/`:

1. **planner** — breaks a goal into milestones with validation criteria. Writes plans to `docs/plans/<slug>.md`. Does not write production code.
2. **generator** — implements one milestone at a time. Writes Kotlin under `sdk/src/main/kotlin/com/sendbird/anthropic/`. Verifies `./gradlew :sdk:build`.
3. **evaluator** — compiles golden-set snippets against the SDK and runs static criteria. Writes reports to `docs/evaluation/<timestamp>.md`. Does not fix code.

Typical loop:
```
user → planner (plan)
user reviews plan
user → generator (implement Mn)
user → evaluator (check against golden set)
loop until evaluator passes
```

## Golden set

`golden-set/cases/*.yaml` is the **contract**. Each case describes a user scenario and a Kotlin snippet that must compile and behave correctly against the SDK. The user owns this directory — agents read it but never modify it.

## Project layout

```
sendbird-anthropic-kotlin/
├── .claude/agents/        # planner, generator, evaluator definitions
├── sdk/                   # the Kotlin SDK module (Gradle)
├── golden-runner/         # Gradle module the evaluator compiles snippets into
├── golden-set/cases/      # *.yaml golden cases (user-owned contract)
└── docs/
    ├── plans/             # planner output
    └── evaluation/        # evaluator output
```
