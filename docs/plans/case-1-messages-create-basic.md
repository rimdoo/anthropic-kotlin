# Plan: case-1-messages-create-basic

> Scope: 단 하나의 golden case (`golden-set/cases/01-messages-create-basic.yaml`) 가 컴파일·통과되는 최소 SDK 를 만든다. 파이프라인 (planner → generator → evaluator) 의 첫 사이클 검증이 본 목적.

## 1. Goal restated

`AnthropicOkHttpClient.builder()...build()` 로 만든 `com.anthropic.client.AnthropicClient` 위에서, 아래 스니펫이 컴파일되고 4개 criteria 가 모두 통과되어야 한다.

```kotlin
val client = AnthropicOkHttpClient.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .build()
val response = client.createMessage(
    model = Model.CLAUDE_OPUS_4_7,
    maxTokens = 1024,
    messages = listOf(Message.user("안녕")),
)
println(response.text)
```

Criteria 요약:
- `suspend-create` — `suspend fun AnthropicClient.createMessage(...)` 존재
- `no-completable-future-leak` — 공개 시그니처에 `CompletableFuture` 노출 없음
- `no-optional-leak` — 공개 시그니처에 `Optional<` 노출 없음
- `response-text-convenience` — `val MessageResponse.text` (확장 또는 멤버) 존재

## 2. Java surface 매핑 (확인 완료)

| 우리 타입 | Java SDK 원본 (anthropic-java-core 2.30.0) | 처리 |
|---|---|---|
| receiver `AnthropicClient` | `com.anthropic.client.AnthropicClient` (sync 인터페이스 — `.messages()` 가 blocking `MessageService` 반환) | bypass (수정 없음) |
| `Model` | `com.anthropic.models.messages.Model` (final class, `CLAUDE_OPUS_4_7` 등 static field 보유) | **typealias 만 노출** (`typealias Model = com.anthropic.models.messages.Model`) |
| `Message` (요청용) | `com.anthropic.models.messages.MessageParam` (`role`, `content` 필드, Java builder 만 노출) | wrapping (`data class Message` + `Message.user(String)` 팩토리) |
| `MessageResponse` (응답용) | `com.anthropic.models.messages.Message` (final class — 응답. `id()`, `content(): List<ContentBlock>`, `usage()`, 다수 `Optional<>` getter) | wrapping (`class MessageResponse` + `val .text` 확장) |

`MessageCreateParams` 는 우리 SDK 사용자에게 직접 노출되지 않음 — 내부에서 Kotlin 파라미터를 받아 빌드한다.

### 결정 사항 (open questions 응답)
- **`Model`**: 그대로 bypass (typealias). 추후 새 모델 정의 필요 시 일반 class 로 추가 (backward compat 유지).
- **`MessageResponse.text`**: 첫 `TextBlock` 의 텍스트, 없으면 빈 문자열 `""`.
- **버전**: Kotlin 2.0.21, kotlinx-coroutines-core 1.9.0, JVM target 17.
- **Java interop**: 모든 top-level 함수 파일에 `@file:JvmName("AnthropicKt")` 통일.
- **Gradle wrapper**: 시스템 gradle 없음 → `chat-android` 의 wrapper 파일 복사 후 `distributionUrl` 을 `gradle-8.10-bin.zip` 으로 갱신.

## 3. Milestones

### M1. Gradle 스캐폴딩

- **What**: 루트 Gradle 프로젝트 + `:sdk` 모듈 (Kotlin JVM)
- **Files**:
  - `settings.gradle.kts` — `rootProject.name = "sendbird-anthropic-kotlin"`, `include(":sdk")`
  - `gradle/libs.versions.toml` — version catalog
  - `build.gradle.kts` (root) — 비어 있거나 plugin 버전만
  - `sdk/build.gradle.kts` — kotlin-jvm 플러그인, JVM 17, `api("com.anthropic:anthropic-java:2.30.0")` (transitive 로 -core, -client-okhttp 포함), `implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:<latest stable>")`
  - `gradle.properties` — `org.gradle.jvmargs=-Xmx2g`
  - `gradle/wrapper/...` — `gradle wrapper --gradle-version 8.10` 으로 생성
- **Java surface mapped**: 없음 (빌드 인프라)
- **Public Kotlin signatures**: 없음
- **Depends on**: 없음
- **Validation**:
  - `./gradlew help` 성공
  - `./gradlew :sdk:compileKotlin` 성공 (빈 모듈)
  - `./gradlew :sdk:dependencies | grep anthropic-java` 가 `2.30.0` 표시

### M2. 핵심 타입: `Model` (typealias), `Message`, `MessageResponse`

- **What**: 스니펫 컴파일에 필요한 타입들. `Model` 은 typealias 만, `Message` / `MessageResponse` 는 단순 wrapping.
- **Files**:
  - `sdk/src/main/kotlin/com/sendbird/anthropic/Model.kt` (typealias 한 줄)
  - `sdk/src/main/kotlin/com/sendbird/anthropic/Message.kt`
  - `sdk/src/main/kotlin/com/sendbird/anthropic/MessageResponse.kt`
- **Java surface mapped**:
  - `com.anthropic.models.messages.Model` → `typealias Model = com.anthropic.models.messages.Model` (bypass)
  - `com.anthropic.models.messages.MessageParam` → `Message` (data class)
  - `com.anthropic.models.messages.Message` (응답) → `MessageResponse` (class)
- **Public Kotlin signatures (illustrative)**:
  ```kotlin
  // Model.kt
  package com.sendbird.anthropic
  typealias Model = com.anthropic.models.messages.Model
  ```
  ```kotlin
  // Message.kt — 요청용 wrapper + user 팩토리
  data class Message internal constructor(internal val raw: MessageParam) {
      companion object {
          fun user(text: String): Message
          // assistant / toolResult 등은 후속 case 에서 추가
      }
  }
  ```
  ```kotlin
  // MessageResponse.kt — 응답 래핑 + .text 편의 프로퍼티
  class MessageResponse internal constructor(internal val raw: JavaResponseMessage) {
      val id: String
      // ... 추가 프로퍼티는 후속 case 에서 노출
  }
  // 빈 문자열 fallback
  val MessageResponse.text: String
      get() = raw.content()
          .firstOrNull { it.text().isPresent }
          ?.text()?.get()?.text() ?: ""
  ```
- **Depends on**: M1
- **Validation**:
  - `./gradlew :sdk:compileKotlin` 성공
  - Criterion `response-text-convenience` 통과: `grep -rE "val.*MessageResponse.*\\.text" sdk/src/main/kotlin` ≥1 match

### M3. `createMessage` extension function

- **What**: `AnthropicClient` 에 붙는 suspend 확장 함수. 내부에서 blocking Java 호출을 `Dispatchers.IO` 로 감싼다.
- **Files**:
  - `sdk/src/main/kotlin/com/sendbird/anthropic/Messages.kt` (파일 상단에 `@file:JvmName("AnthropicKt")` — Java 사용자가 `AnthropicKt.createMessage(client, ...)` 로 부르도록)
- **Java surface mapped**:
  - `AnthropicClient.messages()` → 내부에서 호출
  - `MessageService.create(MessageCreateParams): com.anthropic.models.messages.Message` → 내부에서 호출, 응답을 우리 `MessageResponse` 로 wrap
  - `MessageCreateParams.builder()` → 내부에서 사용 (사용자에게 보이지 않음)
- **Public Kotlin signatures**:
  ```kotlin
  @file:JvmName("AnthropicKt")
  package com.sendbird.anthropic
  // ...

  suspend fun AnthropicClient.createMessage(
      model: Model,
      maxTokens: Int,
      messages: List<Message>,
      system: String? = null,        // case 3 에서 활용, 본 plan 에선 항상 null
      temperature: Double? = null,   // 후속 case 에서 활용
  ): MessageResponse = withContext(Dispatchers.IO) {
      val params = MessageCreateParams.builder()
          .model(model.raw)
          .maxTokens(maxTokens.toLong())
          .messages(messages.map { it.raw })
          .apply { system?.let { system(it) } }
          .apply { temperature?.let { temperature(it) } }
          .build()
      MessageResponse(messages().create(params))
  }
  ```
- **Depends on**: M2
- **Validation** (case 1 의 4개 criteria 전부):
  - `suspend-create`: `grep -rE "suspend fun.*AnthropicClient\\.createMessage" sdk/src/main/kotlin` ≥1
  - `no-completable-future-leak`: `grep -rE "^(?!.*private).*:\\s*CompletableFuture" sdk/src/main/kotlin` == 0
  - `no-optional-leak`: `grep -rE "^(?!.*private).*:\\s*Optional<" sdk/src/main/kotlin` == 0
  - `response-text-convenience`: 이미 M2 에서 통과
  - Compile check: evaluator 가 `:golden-runner` 모듈을 스캐폴딩해 case 1 snippet 을 컴파일 → 성공해야 함

## 4. 검증 시 evaluator 가 수행할 작업 (참고)

`:golden-runner` Gradle 모듈을 첫 호출 시 evaluator 가 스캐폴딩한다:
- `golden-runner/build.gradle.kts` — `implementation(project(":sdk"))` + kotlinx-coroutines (snippet 이 `suspend` 컨텍스트 안에서 동작하도록 wrapper 생성)
- `golden-runner/src/main/kotlin/Case_messages_create_basic.kt` — 다음 wrapper:
  ```kotlin
  import <yaml.imports>
  suspend fun case_messages_create_basic() {
      <yaml.snippet>
  }
  ```
- `./gradlew :golden-runner:compileKotlin` 으로 컴파일 결과 캡처

## 5. Open questions

1. **`MessageResponse.text` semantics** — 첫 텍스트 블록을 반환? 없으면? 추천: 없으면 빈 문자열 `""` (case 1 의 `println(response.text)` 가 깨지지 않게). 후속 case 에서 정밀한 접근이 필요하면 별도 메서드로 제공.
2. **`Model` 의 미지의 모델 처리** — Java SDK 의 `Model` 은 임의의 문자열도 받음 (`Model.of("custom-model")` 가능). 우리 value class 에 `fun custom(id: String): Model` 같은 escape hatch 를 두자. (case 1 에는 불필요하지만 M2 에서 미리 넣어두면 후속 case 에서 0 비용.)
3. **Kotlin / Coroutines 버전** — 추천: Kotlin 2.0.21, kotlinx-coroutines-core 1.9.0, JVM target 17. 변경 의견 있으면 알려주세요.
4. **`@file:JvmName("AnthropicKt")` 위치** — 본 plan 은 `Messages.kt` 파일에 두지만, 후속에 `Streaming.kt` 등이 추가되면 모두 같은 `@file:JvmName("AnthropicKt")` 로 묶을 예정. 의도 OK?
5. **Gradle wrapper** — 사용자가 직접 `gradle wrapper --gradle-version 8.10` 을 실행해야 하나, 아니면 generator 가 `gradle` 명령으로 생성 시도? 시스템에 gradle 이 설치되어 있는지 확인 필요.
