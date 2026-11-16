package com.napsak.app.domain.model

/** Expected, user-facing failures of room operations; the UI maps [kind] to a localized message. */
class RoomError(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NOT_FOUND, VOTING_STARTED, FULL, UNREADABLE }
}
