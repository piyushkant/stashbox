package io.github.kantpiyush.stashbox

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

// Phase 1: a simple in-memory store. In Phase 2 this gets replaced by a
// real database (Spring Data JPA + Postgres). Keeping it behind a small
// class now means the controller won't have to change much when we swap it.
@Component
class StashItemStore {
	private val items = ConcurrentHashMap<String, StashItem>()

	fun findAll(): List<StashItem> = items.values.sortedByDescending { it.createdAt }

	fun findById(id: String): StashItem? = items[id]

	fun save(item: StashItem): StashItem {
		items[item.id] = item
		return item
	}

	fun deleteById(id: String): Boolean = items.remove(id) != null
}
