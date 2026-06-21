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
    // GET /items -> list every stashed item.
    // Example:
    //   curl http://localhost:8080/items
    @GetMapping
    fun getAll(): List<StashItem> = store.findAll()

    // GET /items/{id} -> one item, or 404 if it doesn't exist.
    // Example:
    //   curl http://localhost:8080/items/3836eb3f-0ef9-4f68-9ad6-fa37078469bd
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
    // Example:
    //   curl -X POST http://localhost:8080/items \
    //     -H "Content-Type: application/json" \
    //     -d '{"text":"reply to PM","status":"OPEN"}'
    //   -> {"id":"3836eb3f-...","text":"reply to PM","link":null,"status":"OPEN","createdAt":"2026-06-21T02:49:25.133943Z"}
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
    // Keeps the same id and createdAt; only text/link/status change.
    // Example:
    //   curl -X PUT http://localhost:8080/items/3836eb3f-0ef9-4f68-9ad6-fa37078469bd \
    //     -H "Content-Type: application/json" \
    //     -d '{"text":"replied to PM - all done","status":"DONE"}'
    //   -> {"id":"3836eb3f-...","text":"replied to PM - all done","link":null,"status":"DONE","createdAt":"2026-06-21T02:49:25.133943Z"}
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
    // Example:
    //   curl -i -X DELETE http://localhost:8080/items/30b1fff0-de39-4684-9916-3e866c7f2966
    //   -> HTTP/1.1 204 No Content   (404 if the id doesn't exist)
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val removed = store.deleteById(id)
        return if (removed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
