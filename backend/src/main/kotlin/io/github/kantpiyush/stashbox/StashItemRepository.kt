package io.github.kantpiyush.stashbox

import org.springframework.data.jpa.repository.JpaRepository

// Spring Data gives us all the basic CRUD database methods for free, just by
// declaring this interface. We never write the implementation; Spring generates
// it at runtime. JpaRepository<StashItem, String> means "entity = StashItem,
// its id type = String".
interface StashItemRepository : JpaRepository<StashItem, String> {

	// A "derived query": Spring reads this method NAME and writes the SQL for us.
	// findAllByOrderByCreatedAtDesc -> "SELECT * FROM stash_item ORDER BY created_at DESC".
	fun findAllByOrderByCreatedAtDesc(): List<StashItem>
}
