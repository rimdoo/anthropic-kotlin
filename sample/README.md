# sample

Kotlin translations of the official Anthropic quickstarts.

```bash
export ANTHROPIC_API_KEY=sk-ant-...

# https://platform.claude.com/docs/en/get-started
./gradlew :sample:run

# https://platform.claude.com/docs/en/managed-agents/quickstart
./gradlew :sample:runManagedAgents

# Extra demos (not 1:1 with docs, but useful)
./gradlew :sample:runStreaming
./gradlew :sample:runToolUse
```

| File | Mirrors | Demonstrates |
| --- | --- | --- |
| `Quickstart.kt` | [get-started](https://platform.claude.com/docs/en/get-started) | `createMessage` — single-turn Messages API call |
| `ManagedAgentsQuickstart.kt` | [managed-agents/quickstart](https://platform.claude.com/docs/en/managed-agents/quickstart) | `createAgent` (with toolset) + `createEnvironment` (cloud + unrestricted) + `createSession` + `sendSessionEvents` + `streamSessionEvents` Flow with sealed `SessionStreamEvent` |
| `StreamingDemo.kt` | — | Plain Messages streaming via `streamMessage` Flow |
| `ToolUseDemo.kt` | — | Two-round tool-use roundtrip with a stubbed tool |
