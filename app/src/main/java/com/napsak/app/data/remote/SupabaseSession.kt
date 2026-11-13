package com.napsak.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gives repositories a signed-in (anonymous) user id.
 *
 * Restores the persisted session explicitly (supabase-kt's `awaitInitialization()` never completes
 * when there is no session event, so it must not be used here) and signs in anonymously only when
 * nothing could be restored. A mutex keeps concurrent callers from creating two anonymous users.
 */
@Singleton
class SupabaseSession @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val lock = Mutex()

    suspend fun userId(): String = lock.withLock {
        val auth = supabase.auth
        auth.currentUserOrNull()?.let { return it.id }
        auth.loadFromStorage()
        auth.currentUserOrNull()?.let { return it.id }
        auth.signInAnonymously()
        auth.currentUserOrNull()?.id ?: throw IllegalStateException("Anonymous sign-in failed")
    }
}
