package com.napsak.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.napsak.app.data.remote.NominatimPlaceSearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Live check against OpenStreetMap Nominatim. */
@RunWith(AndroidJUnit4::class)
class PlaceSearchTest {

    @Test
    fun finds_a_well_known_place() = runBlocking<Unit> {
        val places = NominatimPlaceSearchRepository().search("Galata Kulesi İstanbul").getOrThrow()
        assertTrue("no results", places.isNotEmpty())
        val galata = places.first()
        // Galata Tower is around 41.02 N, 28.97 E
        assertTrue("lat ${galata.latitude}", galata.latitude in 40.9..41.2)
        assertTrue("lon ${galata.longitude}", galata.longitude in 28.8..29.2)
        assertTrue(galata.name.isNotBlank())
    }

    @Test
    fun very_short_queries_do_not_hit_the_network() = runBlocking<Unit> {
        assertTrue(NominatimPlaceSearchRepository().search("a").getOrThrow().isEmpty())
    }
}
