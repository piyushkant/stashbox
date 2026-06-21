package io.github.kantpiyush.stashbox

import java.time.Instant
import java.util.UUID

enum class StashStatus {
	TODO,
	REPLY_LATER,
	DONE,
}

// The core item: a Slack message (or any link) you saved to act on later.
data class StashItem(
	val id: String = UUID.randomUUID().toString(),
	val text: String,
	val link: String? = null,
	val status: StashStatus = StashStatus.TODO,
	val createdAt: Instant = Instant.now(),
)

// What the client sends when creating or updating an item.
// Kept separate from StashItem so the server controls id and createdAt.
data class StashItemRequest(
	val text: String,
	val link: String? = null,
	val status: StashStatus = StashStatus.TODO,
)
