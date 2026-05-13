# Golden set

This directory is the **contract**. Each `cases/*.yaml` describes one user scenario; the SDK passes the case when:

1. The `expected_api.snippet` compiles against the `:sdk` module (the evaluator scaffolds it into `:golden-runner`).
2. All `criteria` entries pass (static grep checks plus the implicit compile check).

Agents:
- **planner / generator** read these files to know the target API shape — never modify them.
- **evaluator** runs them and writes reports to `docs/evaluation/`.
- **User** owns this directory.

## Case schema

```yaml
id: <kebab-case-id>            # filename minus .yaml
title: "..."                   # short
description: "..."             # what's being tested
prompt: |                      # human-language scenario
  ...
expected_api:
  imports:                     # imports prepended to the snippet by the runner
    - com.sendbird.anthropic.createMessage
    - ...
  context: suspend             # "suspend" (wrap in suspend fun) or "blocking"
  snippet: |
    val client = AnthropicOkHttpClient.builder().apiKey("test").build()
    ...
criteria:
  - id: <kebab-case-id>
    check: static              # "static" (grep) or "manual"
    description: "..."
    grep_must_match:           # at least one match required (optional)
      path: sdk/src/main
      pattern: "<regex>"
    grep_must_not_match:       # zero matches required (optional)
      path: sdk/src/main
      pattern: "<regex>"
```

Compile (`check: compile`) is implicit for every case — the evaluator always tries to compile the snippet, no need to declare.
