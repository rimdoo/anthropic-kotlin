# sample

Runnable demos exercising the SDK.

```bash
export ANTHROPIC_API_KEY=sk-ant-...

# 1. Single-turn chat (default `:sample:run` target)
./gradlew :sample:run
# with a custom prompt
./gradlew :sample:run --args="'why is the sky blue?'"

# 2. Streaming
./gradlew :sample:runStreaming

# 3. Tool use roundtrip (stubbed tool)
./gradlew :sample:runToolUse
```

Each demo source file lives in `src/main/kotlin/com/rimdoo/anthropic/sample/`:

| File | Demo |
| --- | --- |
| `Quickstart.kt` | `client.createMessage(...)` — basic suspend call |
| `StreamingDemo.kt` | `client.streamMessage(...)` — `Flow<MessageStreamEvent>` |
| `ToolUseDemo.kt` | tool declaration + two-round roundtrip with stubbed tool result |
