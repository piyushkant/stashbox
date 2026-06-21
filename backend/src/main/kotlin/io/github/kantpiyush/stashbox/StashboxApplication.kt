package io.github.kantpiyush.stashbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StashboxApplication

fun main(args: Array<String>) {
	runApplication<StashboxApplication>(*args)
}
