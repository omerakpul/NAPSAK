package com.napsak.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.napsak.app.data.datasource.UserPreferencesDataSource
import com.napsak.app.data.remote.SupabaseSession
import com.napsak.app.data.repository.RoomRepositoryImpl
import com.napsak.app.domain.model.Choice
import com.napsak.app.domain.model.RoomState
import com.napsak.app.domain.model.VoteMode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import android.util.Log
import com.napsak.app.domain.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test against the live Supabase project: two anonymous users (separate in-memory
 * sessions) go through lobby → voting → result, observing each other through Realtime.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseRoomFlowTest {

    private fun newClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) { sessionManager = MemorySessionManager() }
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    private fun repo(client: SupabaseClient): RoomRepositoryImpl {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        return RoomRepositoryImpl(client, SupabaseSession(client), UserPreferencesDataSource(ctx), com.napsak.app.data.remote.ConnectivityObserver(ctx))
    }

    private fun step(name: String) = Log.i("E2E", name)

    /** Waits for a realtime-driven condition, failing with a readable message instead of hanging. */
    private suspend fun Flow<Room?>.await(what: String, predicate: (Room?) -> Boolean): Room? {
        step("waiting: $what")
        return try {
            withTimeout(20_000) { first(predicate) }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Timed out waiting for: $what")
        }
    }

    @Test
    fun default_session_manager_status() = runBlocking<Unit> {
        withTimeout(60_000) {
            val client = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
                install(Auth)
            }
            val restored = client.auth.loadFromStorage()
            step("default: loadFromStorage=$restored status=${client.auth.sessionStatus.value} user=${client.auth.currentUserOrNull()?.id}")
            if (client.auth.currentUserOrNull() == null) client.auth.signInAnonymously()
            val id1 = client.auth.currentUserOrNull()?.id
            step("default: signed in $id1 status=${client.auth.sessionStatus.value}")
            // a second client sharing the same default storage must restore the same user
            val client2 = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) { install(Auth) }
            val restored2 = client2.auth.loadFromStorage()
            step("default: second client restored=$restored2 user=${client2.auth.currentUserOrNull()?.id}")
            org.junit.Assert.assertEquals(id1, client2.auth.currentUserOrNull()?.id)
        }
    }

    @Test
    fun suggestions_and_vote_modes() = runBlocking<Unit> {
        withTimeout(150_000) {
            val host = repo(newClient())
            val guest = repo(newClient())
            val roomId = host.createRoom("Ali").first().getOrThrow().id
            guest.joinRoom(roomId, "Veli").first().getOrThrow()

            // guest proposes an option: the host sees it, the guest sees only their own
            step("suggest")
            guest.suggestChoice(roomId, "Sushi Bar", "Önerim").getOrThrow()
            val seenByHost = host.observeRoom(roomId).await("host sees suggestion") { r -> r?.suggestions?.size == 1 }!!
            assertEquals("Sushi Bar", seenByHost.suggestions.single().name)
            assertEquals(1, guest.observeRoom(roomId).await("guest sees own suggestion") { r -> r?.suggestions?.size == 1 }!!.suggestions.size)
            // the host can dismiss it
            host.dismissSuggestion(roomId, seenByHost.suggestions.single().id).getOrThrow()
            host.observeRoom(roomId).await("suggestion dismissed") { r -> r?.suggestions?.isEmpty() == true }

            // RANK: Borda points (3 options -> 3/2/1)
            val choices = listOf(Choice(id = "a", name = "Pizza"), Choice(id = "b", name = "Burger"), Choice(id = "c", name = "Sushi"))
            host.startVotingWithChoices(roomId, choices, VoteMode.RANK).getOrThrow()
            val ranking = guest.observeRoom(roomId).await("guest sees RANK room") { it?.state == RoomState.VOTING }!!
            assertEquals(VoteMode.RANK, ranking.voteMode)
            assertTrue("late suggestions are rejected", guest.suggestChoice(roomId, "Geç kaldı").isFailure)
            assertTrue("duplicates are rejected", guest.submitVotes(roomId, listOf("a", "a", "b")).isFailure)
            host.submitVotes(roomId, listOf("c", "a", "b")).getOrThrow()   // c=3 a=2 b=1
            guest.submitVotes(roomId, listOf("a", "c", "b")).getOrThrow()  // a=3 c=2 b=1
            val tally = host.observeRoom(roomId).await("all voted") { r -> r != null && r.participants.values.all { it.hasVoted } }!!
            assertEquals(5, tally.choices["a"]?.voteCount)
            assertEquals(5, tally.choices["c"]?.voteCount)
            assertEquals(2, tally.choices["b"]?.voteCount)
            host.deleteRoom(roomId).getOrThrow()

            // SINGLE: exactly one pick
            val room2 = host.createRoom("Ali").first().getOrThrow().id
            guest.joinRoom(room2, "Veli").first().getOrThrow()
            host.startVotingWithChoices(room2, choices, VoteMode.SINGLE).getOrThrow()
            assertTrue("two picks are rejected", guest.submitVotes(room2, listOf("a", "b")).isFailure)
            guest.submitVotes(room2, listOf("b")).getOrThrow()
            host.submitVotes(room2, listOf("b")).getOrThrow()
            val single = host.observeRoom(room2).await("single tally") { r -> r != null && r.participants.values.all { it.hasVoted } }!!
            assertEquals(2, single.choices["b"]?.voteCount)
            assertEquals("b", host.endVotingAndSetWinner(room2, null).getOrThrow())
            host.deleteRoom(room2).getOrThrow()
        }
    }

    @Test
    fun image_upload_returns_a_public_url() = runBlocking<Unit> {
        withTimeout(60_000) {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val file = java.io.File(ctx.cacheDir, "e2e_${System.nanoTime()}.png")
            android.graphics.Bitmap.createBitmap(1600, 1200, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.MAGENTA)
                file.outputStream().use { compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
            val client = newClient()
            val repo = com.napsak.app.data.repository.ImageRepositoryImpl(client, SupabaseSession(client))

            val result = repo.uploadImage(ctx, android.net.Uri.fromFile(file))
            val url = (result as com.napsak.app.domain.model.UploadResult.Success).url
            assertTrue(url, url.startsWith(BuildConfig.SUPABASE_URL))

            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            assertEquals(200, conn.responseCode)
            assertEquals("image/jpeg", conn.contentType)

            // Coil 3 (with the OkHttp network fetcher) must be able to load the same URL
            val loaded = coil3.ImageLoader(ctx).execute(coil3.request.ImageRequest.Builder(ctx).data(url).build())
            assertTrue("Coil failed: $loaded", loaded is coil3.request.SuccessResult)
            // clean up: the uploader owns the object and may delete it
            client.storage.from("choice-images").delete(url.substringAfter("/choice-images/"))
            file.delete()
        }
    }

    @Test
    fun two_users_play_a_full_round() = runBlocking<Unit> {
        withTimeout(150_000) {
            val host = repo(newClient())
            val guest = repo(newClient())

            step("create")
            val created = host.createRoom("Ali").first().getOrThrow()
            val roomId = created.id
            assertEquals(6, roomId.length)
            assertEquals(1, created.participants.size)

            val joined = guest.joinRoom(roomId, "Veli").first().getOrThrow()
            assertEquals(2, joined.participants.size)
            assertTrue(guest.joinRoom("000000", "X").first().isFailure)

            // realtime: host sees the guest become ready
            step("joined")
            assertEquals("guest can resume the lobby", roomId, guest.findResumableRoomId())
            val guestId = joined.participants.keys.first { it != created.hostId }
            guest.setParticipantReady(roomId, guestId, true).getOrThrow()
            val ready = host.observeRoom(roomId).await("host sees guest ready") { r -> r?.participants?.get(guestId)?.isReady == true }
            assertNotNull(ready)

            // start voting (host) → guest observes VOTING with ordered choices
            val choices = listOf(
                Choice(id = "a", name = "Pizza"),
                Choice(id = "b", name = "Sushi", latitude = 41.0, longitude = 29.0)
            )
            assertTrue("guest must not start voting", guest.startVotingWithChoices(roomId, choices).isFailure)
            host.startVotingWithChoices(roomId, choices).getOrThrow()
            val voting = guest.observeRoom(roomId).await("guest sees VOTING") { it?.state == RoomState.VOTING }!!
            assertEquals(listOf("a", "b"), voting.choices.keys.toList())

            // votes: one vote each, double vote rejected
            host.submitVotes(roomId, listOf("a", "b")).getOrThrow()
            guest.submitVotes(roomId, listOf("a")).getOrThrow()
            assertTrue(guest.submitVotes(roomId, listOf("b")).isFailure)
            val allVoted = host.observeRoom(roomId).await("host sees all voted") { r -> r != null && r.participants.values.all { it.hasVoted } }!!
            assertEquals(2, allVoted.choices["a"]?.voteCount)
            assertEquals(1, allVoted.choices["b"]?.voteCount)

            assertNull("a finished voter has nothing to resume", host.findResumableRoomId())

            // end: guest cannot, host can; both see the same winner
            assertTrue(guest.endVotingAndSetWinner(roomId, "a").isFailure)
            assertEquals("a", host.endVotingAndSetWinner(roomId, "a").getOrThrow())
            val result = guest.observeRoom(roomId).await("guest sees RESULT") { it?.state == RoomState.RESULT }!!
            assertEquals("a", result.winnerChoiceId)

            // guest leaves, host deletes → room disappears for everyone
            guest.leaveRoom(roomId).getOrThrow()
            val afterLeave = host.observeRoom(roomId).await("host sees guest left") { r -> r != null && r.participants.size == 1 }
            assertNotNull(afterLeave)
            host.deleteRoom(roomId).getOrThrow()
            assertNull(host.observeRoom(roomId).await("room deleted") { it == null })
            assertNull(host.findResumableRoomId())
        }
    }
}
