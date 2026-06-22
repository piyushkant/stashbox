package io.github.kantpiyush.stashbox

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

// The shapes of the JSON we exchange with the Python AI service.
// These mirror SummarizeRequest / SummarizeResponse in ai/main.py.
data class SummarizeRequest(val text: String)
data class SummarizeResponse(val summary: String)

// Contract for talking to the AI service (interface, per our convention).
interface AiSummaryClient {
	fun summarize(text: String): String
}

// Real implementation: makes an OUTBOUND HTTP POST to the Python service.
// This is the new idea in Phase 5 - so far the backend only RECEIVED requests;
// here it SENDS one, the same way ai/main.py calls Ollama.
@Component
class AiSummaryClientImpl(
	// Inject Spring's auto-configured RestClient.Builder (NOT the static
	// RestClient.builder()). The auto-configured one uses Spring's ObjectMapper,
	// which has the Kotlin module registered, so it can serialize Kotlin data
	// classes correctly. The plain static builder uses a vanilla ObjectMapper that
	// serializes Kotlin data classes to empty {} - which caused a 422 here.
	builder: RestClient.Builder,
	// @Value injects the config value from application.properties.
	@Value("\${stashbox.ai.base-url}") baseUrl: String,
) : AiSummaryClient {

	// RestClient is Spring's modern synchronous HTTP client (comes with spring-web,
	// no extra dependency). We pin its base URL once here. We force the simple
	// JDK URLConnection-based request factory; the default JDK HttpClient was
	// sending a connection-upgrade header that uvicorn rejected as an invalid request.
	private val restClient = builder
		.baseUrl(baseUrl)
		.requestFactory(SimpleClientHttpRequestFactory())
		.build()

	override fun summarize(text: String): String {
		val response = restClient.post()
			.uri("/summarize")                       // -> POST {baseUrl}/summarize
			.contentType(MediaType.APPLICATION_JSON) // tell the server the body is JSON
			.body(SummarizeRequest(text))            // serialized to JSON automatically
			.retrieve()                              // send the request, get the response
			.body(SummarizeResponse::class.java)     // parse JSON back into our class
		return response?.summary ?: ""
	}
}
