package com.mawidplus.patient.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseAvailableDaysTest {

    @Test
    fun `array of tokens maps to Arabic labels`() {
        val el = Json.parseToJsonElement("""["0","mon"]""")
        assertEquals(listOf("الأحد", "الإثنين"), parseAvailableDaysFromJson(el))
    }

    @Test
    fun `null returns empty`() {
        assertEquals(emptyList<String>(), parseAvailableDaysFromJson(null))
    }
}
