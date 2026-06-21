package io.github.kantpiyush.stashbox

import org.springframework.stereotype.Service

// The service contract (like an iOS protocol). Controllers depend on THIS, not on
// the concrete class, so a fake implementation can be swapped in for tests.
// Structure: Controller (HTTP) -> StashItemService (logic) -> StashItemRepository (DB).
interface StashItemService {
	fun findAll(): List<StashItem>
	fun findById(id: String): StashItem?
	fun save(item: StashItem): StashItem
	fun deleteById(id: String): Boolean
}

// The real implementation, backed by the database via the repository. For now it
// mostly delegates, but business rules (e.g. per-user scoping in the auth phase)
// will live here.
@Service
class StashItemServiceImpl(
	private val repository: StashItemRepository,
) : StashItemService {

	override fun findAll(): List<StashItem> = repository.findAllByOrderByCreatedAtDesc()

	// JpaRepository.findById returns Optional<StashItem> (a Java type); .orElse(null)
	// turns "empty" into a Kotlin null so callers can use null checks / the Elvis operator.
	override fun findById(id: String): StashItem? = repository.findById(id).orElse(null)

	override fun save(item: StashItem): StashItem = repository.save(item)

	// existsById avoids a delete that silently does nothing; we return whether a
	// row was actually removed so the controller can answer 204 vs 404.
	override fun deleteById(id: String): Boolean {
		if (!repository.existsById(id)) return false
		repository.deleteById(id)
		return true
	}
}
