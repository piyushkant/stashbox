package io.github.kantpiyush.stashbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(StashItemController::class)
class StashItemControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var service: StashItemService

    @MockitoBean
    private lateinit var aiSummaryClient: AiSummaryClient

    // --- GET /items ---

    @Test
    fun `GET items returns 200 with empty list`() {
        whenever(service.findAll()).thenReturn(emptyList())

        mockMvc.perform(get("/items"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))
    }

    @Test
    fun `GET items returns 200 with all items`() {
        val items = listOf(StashItem(text = "buy milk"), StashItem(text = "call mom"))
        whenever(service.findAll()).thenReturn(items)

        mockMvc.perform(get("/items"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].text").value("buy milk"))
            .andExpect(jsonPath("$[1].text").value("call mom"))
    }

    // --- GET /items/{id} ---

    @Test
    fun `GET items by id returns 200 when found`() {
        val item = StashItem(id = "abc", text = "found item")
        whenever(service.findById("abc")).thenReturn(item)

        mockMvc.perform(get("/items/abc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("abc"))
            .andExpect(jsonPath("$.text").value("found item"))
    }

    @Test
    fun `GET items by id returns 404 when not found`() {
        whenever(service.findById("missing")).thenReturn(null)

        mockMvc.perform(get("/items/missing"))
            .andExpect(status().isNotFound)
    }

    // --- POST /items ---

    @Test
    fun `POST items returns 201 with created item`() {
        val request = StashItemRequest(text = "new task")
        val saved = StashItem(text = "new task", status = StashStatus.OPEN)
        whenever(service.save(any())).thenReturn(saved)

        mockMvc.perform(
            post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.text").value("new task"))
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    fun `POST items with link returns 201 with link set`() {
        val request = StashItemRequest(text = "check this", link = "https://example.com")
        val saved = StashItem(text = "check this", link = "https://example.com")
        whenever(service.save(any())).thenReturn(saved)

        mockMvc.perform(
            post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.link").value("https://example.com"))
    }

    // --- PUT /items/{id} ---

    @Test
    fun `PUT items by id returns 200 with updated item`() {
        val existing = StashItem(id = "abc", text = "old text", status = StashStatus.OPEN)
        val request = StashItemRequest(text = "updated text", status = StashStatus.DONE)
        val saved = StashItem(id = "abc", text = "updated text", status = StashStatus.DONE)
        whenever(service.findById("abc")).thenReturn(existing)
        whenever(service.save(any())).thenReturn(saved)

        mockMvc.perform(
            put("/items/abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("updated text"))
            .andExpect(jsonPath("$.status").value("DONE"))
    }

    @Test
    fun `PUT items by id returns 404 when not found`() {
        whenever(service.findById("missing")).thenReturn(null)

        mockMvc.perform(
            put("/items/missing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(StashItemRequest(text = "anything")))
        )
            .andExpect(status().isNotFound)
    }

    // --- DELETE /items/{id} ---

    @Test
    fun `DELETE items by id returns 204 when deleted`() {
        whenever(service.deleteById("abc")).thenReturn(true)

        mockMvc.perform(delete("/items/abc"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE items by id returns 404 when not found`() {
        whenever(service.deleteById("missing")).thenReturn(false)

        mockMvc.perform(delete("/items/missing"))
            .andExpect(status().isNotFound)
    }

    // --- POST /items/{id}/summarize ---

    @Test
    fun `POST summarize returns 200 with summary on item`() {
        val item = StashItem(id = "abc", text = "long task description")
        val saved = StashItem(id = "abc", text = "long task description", summary = "short summary")
        whenever(service.findById("abc")).thenReturn(item)
        whenever(aiSummaryClient.summarize("long task description")).thenReturn("short summary")
        whenever(service.save(any())).thenReturn(saved)

        mockMvc.perform(post("/items/abc/summarize"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary").value("short summary"))
    }

    @Test
    fun `POST summarize returns 200 with empty summary when AI is unavailable`() {
        val item = StashItem(id = "abc", text = "some text")
        val saved = StashItem(id = "abc", text = "some text", summary = "")
        whenever(service.findById("abc")).thenReturn(item)
        whenever(aiSummaryClient.summarize("some text")).thenReturn("")
        whenever(service.save(any())).thenReturn(saved)

        mockMvc.perform(post("/items/abc/summarize"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.summary").value(""))
    }

    @Test
    fun `POST summarize returns 404 when item not found`() {
        whenever(service.findById("missing")).thenReturn(null)

        mockMvc.perform(post("/items/missing/summarize"))
            .andExpect(status().isNotFound)
    }
}
