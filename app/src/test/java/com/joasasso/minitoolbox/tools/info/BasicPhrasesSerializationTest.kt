package com.joasasso.minitoolbox.tools.info

import com.joasasso.minitoolbox.data.Frase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BasicPhrasesSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `roundtrip serialization for Frase`() {
        val original = Frase(
            categoria = "greetings",
            traducciones = mapOf(
                "es" to "Hola",
                "en" to "Hello",
                "fr" to "Bonjour"
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Frase>(encoded)

        assertEquals(original, decoded)
        assertEquals("greetings", decoded.categoria)
        assertEquals("Hola", decoded.traducciones["es"])
        assertEquals("Hello", decoded.traducciones["en"])
    }

    @Test
    fun `decode sample basic phrases list`() {
        val sampleJson = """
            [
              {
                "categoria": "greetings",
                "traducciones": {
                  "es": "Hola",
                  "en": "Hello"
                }
              },
              {
                "categoria": "food",
                "traducciones": {
                  "es": "Agua",
                  "en": "Water"
                }
              }
            ]
        """.trimIndent()

        val phrases = json.decodeFromString<List<Frase>>(sampleJson)
        assertEquals(2, phrases.size)
        assertEquals("greetings", phrases[0].categoria)
        assertEquals("Hola", phrases[0].traducciones["es"])
        assertEquals("food", phrases[1].categoria)
        assertEquals("Water", phrases[1].traducciones["en"])
    }

    @Test
    fun `production basic_phrases json asset deserializes correctly`() {
        val assetFile = File("src/main/assets/basic_phrases.json")
        assertTrue("Asset file must exist", assetFile.exists())

        val jsonContent = assetFile.readText()
        val phrases = json.decodeFromString<List<Frase>>(jsonContent)

        assertFalse("Phrases list must not be empty", phrases.isEmpty())
        assertEquals("Expected 36 phrases in catalogue", 36, phrases.size)

        for (frase in phrases) {
            assertNotNull("Category must not be null", frase.categoria)
            assertTrue("Category must not be blank", frase.categoria.isNotBlank())
            assertFalse("Translations must not be empty", frase.traducciones.isEmpty())
            assertTrue("Must contain Spanish translation", frase.traducciones.containsKey("es"))
            assertTrue("Must contain English translation", frase.traducciones.containsKey("en"))
        }
    }
}
