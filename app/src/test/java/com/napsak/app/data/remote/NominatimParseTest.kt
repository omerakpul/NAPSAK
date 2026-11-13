package com.napsak.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimParseTest {

    @Test
    fun `parses name, address and coordinates`() {
        val json = """[
          {"place_id":1,"lat":"41.0082","lon":"28.9784","name":"Çiya Sofrası",
           "display_name":"Çiya Sofrası, Güneşli Bahçe Sokağı, Kadıköy, İstanbul, Türkiye","extra":{}},
          {"place_id":2,"lat":"40.99","lon":"29.03","name":"",
           "display_name":"Moda Sahili, Kadıköy, İstanbul"}
        ]"""
        val places = parsePlaces(json)
        assertEquals(2, places.size)
        assertEquals("Çiya Sofrası", places[0].name)
        assertEquals("Güneşli Bahçe Sokağı, Kadıköy, İstanbul, Türkiye", places[0].address)
        assertEquals(41.0082, places[0].latitude, 1e-9)
        assertEquals(28.9784, places[0].longitude, 1e-9)
        // a missing name falls back to the first part of display_name
        assertEquals("Moda Sahili", places[1].name)
    }

    @Test
    fun `entries without usable coordinates are skipped`() {
        val json = """[{"lat":"x","lon":"1","name":"Bad"},{"lat":"1","lon":"2","name":"Good","display_name":"Good, Town"}]"""
        assertEquals(listOf("Good"), parsePlaces(json).map { it.name })
    }

    @Test
    fun `empty result is an empty list`() {
        assertTrue(parsePlaces("[]").isEmpty())
    }
}
