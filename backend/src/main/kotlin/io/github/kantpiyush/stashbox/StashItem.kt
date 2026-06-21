package io.github.kantpiyush.stashbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class StashStatus {
	OPEN,
	DONE,
}

// The core item: a Slack message (or any link) you saved to act on later.
// This is a JPA entity now: each instance maps to one row in the "stash_item" table.
@Entity
@Table(name = "stash_item")
class StashItem(
	@Id
	var id: String = UUID.randomUUID().toString(),

	@Column(nullable = false)
	var text: String = "",

	var link: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: StashStatus = StashStatus.OPEN,

	@Column(nullable = false)
	var createdAt: Instant = Instant.now(),
)

// What the client sends when creating or updating an item.
// Kept separate from StashItem so the server controls id and createdAt.
data class StashItemRequest(
	val text: String,
	val link: String? = null,
	val status: StashStatus = StashStatus.OPEN,
)
