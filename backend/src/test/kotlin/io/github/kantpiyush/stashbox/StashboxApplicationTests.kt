package io.github.kantpiyush.stashbox

// Full Spring context tests (e.g. @SpringBootTest) are intentionally omitted.
// They require a live database and are not suitable for CI without a test DB container.
// Unit tests in StashItemServiceTest and StashItemControllerTest cover the logic instead:
//   - StashItemServiceTest  -> service layer (pure unit, no Spring, mocked repository)
//   - StashItemControllerTest -> HTTP layer (@WebMvcTest, mocked service, no DB needed)
