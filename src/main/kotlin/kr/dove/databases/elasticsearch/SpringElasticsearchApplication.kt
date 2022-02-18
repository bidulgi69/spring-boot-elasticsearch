package kr.dove.databases.elasticsearch

import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import reactor.core.publisher.Mono

@SpringBootApplication
@ConfigurationProperties(prefix = "elasticsearch.index")
class SpringElasticsearchApplication(
	private val reactiveElasticsearchClient: ReactiveElasticsearchClient,
) {
	lateinit var name: String
	var shards: Int? = null
	var replicas: Int? = null

	//	Create an index
	//	Bean annotation is intended to execute
	//	when the application starts running.
	@Bean
	fun createIndex() {
		reactiveElasticsearchClient.indices().getIndex(GetIndexRequest(name))
			.onErrorResume { Mono.empty() } //  if index does not exist, move to switchIfEmpty function.
			.flatMap { println("Index already exists. $name"); Mono.just(true) }
			.switchIfEmpty(Mono.defer {
				val createIndexRequest = CreateIndexRequest(name)
				createIndexRequest.settings(
					Settings.builder()
						.put("index.number_of_shards", shards ?: 1)
						.put("index.number_of_replicas", replicas ?: 0)
				)
				createIndexRequest.source("{\n" +
						"  \"settings\": {\n" +
						"    \"max_ngram_diff\": \"3\",\n" +
						"    \"analysis\": {\n" +
						"      \"analyzer\": {\n" +
						"        \"word_analyzer\": {\n" +
						"          \"tokenizer\": \"text_tokenizer\",\n" +
						"          \"filter\": [\n" +
						"            \"lowercase\"\n" +
						"          ]\n" +
						"        }\n" +
						"      },\n" +
						"      \"tokenizer\": {\n" +
						"        \"text_tokenizer\": {\n" +
						"          \"type\": \"ngram\",\n" +
						"          \"min_gram\": 2,\n" +
						"          \"max_gram\": 5,\n" +
						"          \"token_chars\": [\n" +
						"            \"letter\",\n" +
						"            \"digit\",\n" +
						"            \"symbol\",\n" +
						"            \"punctuation\"\n" +
						"          ]\n" +
						"        }\n" +
						"      }\n" +
						"    }" +
						"  }, \n" +
						"\"mappings\": {\n" +
						"    \"properties\": {\n" +
						"      \"boardId\": {\n" +
						"        \"type\": \"text\"\n" +
						"      },\n" +
						"      \"title\": {\n" +
						"        \"type\": \"text\",\n" +
						"        \"analyzer\": \"word_analyzer\"\n" +
						"      },\n" +
						"      \"content\": {\n" +
						"        \"type\": \"text\",\n" +
						"        \"analyzer\": \"whitespace\"\n" +
						"      },\n" +
						"      \"writer\": {\n" +
						"        \"type\": \"keyword\"\n" +
						"      },\n" +
						"      \"password\": {\n" +
						"        \"type\": \"text\"\n" +
						"      },\n" +
						"      \"comments\": {\n" +
						"        \"properties\": {\n" +
						"          \"boardId\": {\n" +
						"            \"type\": \"keyword\"\n" +
						"          },\n" +
						"          \"writer\": {\n" +
						"            \"type\": \"text\"\n" +
						"          },\n" +
						"          \"password\": {\n" +
						"            \"type\": \"text\"\n" +
						"          },\n" +
						"          \"content\": {\n" +
						"            \"type\": \"text\",\n" +
						"            \"analyzer\": \"whitespace\"\n" +
						"          },\n" +
						"          \"created\": {\n" +
						"            \"type\": \"long\"\n" +
						"          }\n" +
						"        }\n" +
						"      },\n" +
						"      \"like\": {\n" +
						"        \"type\": \"long\"\n" +
						"      },\n" +
						"      \"dislike\": {\n" +
						"        \"type\": \"long\"\n" +
						"      },\n" +
						"      \"created\": {\n" +
						"        \"type\": \"long\"\n" +
						"      }\n" +
						"    }\n" +
						"  }\n" +
						"}\n", XContentType.JSON)
				reactiveElasticsearchClient
					.indices()
					.createIndex(createIndexRequest)
			}).subscribe { println("Check Index($name) on start executed.") }	//	subscribe to publisher (Call)
	}
}

fun main(args: Array<String>) {
	runApplication<SpringElasticsearchApplication>(*args)
}
