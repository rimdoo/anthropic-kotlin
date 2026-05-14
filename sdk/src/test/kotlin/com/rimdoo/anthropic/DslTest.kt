package com.rimdoo.anthropic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Exercise the two public DSL entry points and verify they build a valid Java SDK union. */
class DslTest {

    // -------- jsonSchema -------- //

    @Test fun `jsonSchema empty builds`() {
        jsonSchema { }
    }

    @Test fun `jsonSchema with a single required string property builds`() {
        val schema = jsonSchema {
            property("city", type = "string", required = true)
        }
        // .raw is internal but accessing forces .build() through our InputSchema wrapper
        assertNotNull(schema)
    }

    @Test fun `jsonSchema with multiple properties of mixed required builds`() {
        jsonSchema {
            property("city", type = "string", required = true, description = "City name")
            property("country", type = "string", required = false)
            property("limit", type = "integer", required = false)
        }
    }

    // -------- systemPrompt -------- //

    @Test fun `systemPrompt empty builds`() {
        systemPrompt { }
    }

    @Test fun `systemPrompt with plain text block builds`() {
        systemPrompt {
            text("You are a helpful assistant.")
        }
    }

    @Test fun `systemPrompt with cache control builds`() {
        systemPrompt {
            text("Long static prefix...", cache = CacheControl.Ephemeral)
        }
    }

    @Test fun `systemPrompt with multiple text blocks builds`() {
        systemPrompt {
            text("Part 1 — long static.", cache = CacheControl.Ephemeral)
            text("Part 2 — dynamic suffix.")
        }
    }
}
