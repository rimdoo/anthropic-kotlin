package com.sendbird.anthropic

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Tool as RawTool

data class Tool(
    val name: String,
    val description: String,
    val inputSchema: InputSchema,
) {
    internal val raw: RawTool by lazy {
        RawTool.builder()
            .name(name)
            .description(description)
            .inputSchema(inputSchema.raw)
            .build()
    }
}

class InputSchema internal constructor(
    internal val raw: RawTool.InputSchema,
)

class JsonSchemaBuilder {
    private val properties = mutableMapOf<String, JsonValue>()
    private val requiredFields = mutableListOf<String>()

    fun property(
        name: String,
        type: String,
        required: Boolean = false,
        description: String? = null,
    ) {
        val propDef = buildMap<String, Any?> {
            put("type", type)
            if (description != null) put("description", description)
        }
        properties[name] = JsonValue.from(propDef)
        if (required) requiredFields.add(name)
    }

    internal fun build(): InputSchema {
        val rawProps = RawTool.InputSchema.Properties.builder()
            .additionalProperties(properties)
            .build()
        val rawSchema = RawTool.InputSchema.builder()
            .properties(rawProps)
            .required(requiredFields)
            .build()
        return InputSchema(rawSchema)
    }
}

fun jsonSchema(build: JsonSchemaBuilder.() -> Unit): InputSchema =
    JsonSchemaBuilder().apply(build).build()
