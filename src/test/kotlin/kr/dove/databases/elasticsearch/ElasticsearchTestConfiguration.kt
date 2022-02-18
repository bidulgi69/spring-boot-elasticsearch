package kr.dove.databases.elasticsearch

import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.core.io.ClassPathResource
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.testcontainers.shaded.org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

@TestConfiguration
@EnableElasticsearchRepositories
class ElasticsearchTestConfiguration(
    @Value("\${elasticsearch.index.name:board}") private val defaultIndexName: String,
): AbstractReactiveElasticsearchConfiguration() {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean("reactiveElasticsearchTestClient", "reactiveElasticsearchClient")
    override fun reactiveElasticsearchClient(): ReactiveElasticsearchClient {
        val clientConfiguration: ClientConfiguration =
            ClientConfiguration
                .builder()
                .connectedTo(ElasticsearchTestContainer.CONTAINER.httpHostAddress)
                .build()
        return ReactiveRestClients.create(clientConfiguration)
            .apply {    //  put index before using
                val createIndexRequest = CreateIndexRequest(defaultIndexName)
                    .mapping(getResource("elasticsearch/mappings/mappings.json"), XContentType.JSON)
                    .settings(getResource("elasticsearch/settings/settings.json"), XContentType.JSON)
                this.indices()
                    .createIndex(createIndexRequest)
                    .subscribe {
                        logger.info("Sending create-index request...")
                    }
            }
    }

    @Bean("reactiveElasticsearchTestTemplate", "reactiveElasticsearchTemplate")
    @DependsOn("reactiveElasticsearchTestClient")
    fun reactiveElasticsearchTemplate(): ReactiveElasticsearchOperations {
        return ReactiveElasticsearchTemplate(reactiveElasticsearchClient())
    }

    private fun getResource(path: String): String {
        val resource = ClassPathResource(path)
        return  IOUtils.toString(resource.inputStream, StandardCharsets.UTF_8)
    }
}