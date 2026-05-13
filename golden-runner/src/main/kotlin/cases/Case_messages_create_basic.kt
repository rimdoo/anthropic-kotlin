// Generated from golden-set/cases/01-messages-create-basic.yaml
// DO NOT EDIT — evaluator regenerates this file each run.
package cases

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.sendbird.anthropic.*

@Suppress("UNUSED_VARIABLE")
suspend fun case_messages_create_basic() {
    val client = AnthropicOkHttpClient.builder()
        .apiKey(System.getenv("ANTHROPIC_API_KEY"))
        .build()
    val response = client.createMessage(
        model = Model.CLAUDE_OPUS_4_7,
        maxTokens = 1024,
        messages = listOf(Message.user("안녕")),
    )
    println(response.text)
}
