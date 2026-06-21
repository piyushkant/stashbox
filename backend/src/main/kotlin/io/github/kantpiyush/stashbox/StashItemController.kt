package io.github.kantpiyush.stashbox

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/items")
class StashItemController(
    private val store: StashItemStore,
) {
    // --- Worked examples (already done) ---------------------------------

    // GET /items -> list every stashed item.
    @GetMapping
    fun getAll(): List<StashItem> = store.findAll()

    // GET /items/{id} -> one item, or 404 if it doesn't exist.
    @GetMapping("/{id}")
    fun getOne(@PathVariable id: String): ResponseEntity<StashItem> {
        val item = store.findById(id)
        return if (item != null) {
            ResponseEntity.ok(item)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // POST /items -> create a new item from the request body, return 201.
    @PostMapping
    fun create(@RequestBody request: StashItemRequest): ResponseEntity<StashItem> {
        val item = StashItem(
            text = request.text,
            link = request.link,
            status = request.status,
        )
        val saved = store.save(item)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    // PUT /items/{id} -> update an existing item, or 404 if missing.
    // Hint: look it up first; if found, copy() it with the new fields
    // (keep the same id and createdAt), save, and return ResponseEntity.ok(...).
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody request: StashItemRequest,
    ): ResponseEntity<StashItem> {
        val existing = store.findById(id)
            ?: return ResponseEntity.notFound().build()

        val updated = existing.copy(
            text = request.text,
            link = request.link,
            status = request.status,
        )
        val saved = store.save(updated)
        return ResponseEntity.ok(saved)
    }

    // DELETE /items/{id} -> remove an item, return 204, or 404 if missing.
    // Hint: store.deleteById(id) returns true if something was removed.
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        // TODO(you): implement delete
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }
}
