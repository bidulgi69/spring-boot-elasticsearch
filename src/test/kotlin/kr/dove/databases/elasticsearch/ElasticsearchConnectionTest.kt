package kr.dove.databases.elasticsearch

import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import reactor.test.StepVerifier

@SpringBootTest(
    classes = [ElasticsearchTestConfiguration::class]
)
class ElasticsearchConnectionTest(
    @Autowired private val reactiveElasticsearchTestClient: ReactiveElasticsearchClient,
) {

    @Test
    @DisplayName("Connection test")
    fun connect() {
        StepVerifier
            .create(
                reactiveElasticsearchTestClient
                    .indices()
                    .getIndex(GetIndexRequest("board"))
            )
            .assertNext { response ->
                Assertions.assertTrue(
                    response.aliases.keys.contains("board")
                )
            }
            .verifyComplete()
    }
}