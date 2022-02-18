package kr.dove.databases.elasticsearch

import kr.dove.databases.elasticsearch.persistence.Board
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.time.ZoneOffset

@SpringBootTest(
    classes = [ElasticsearchTestConfiguration::class]
)
class ElasticsearchPersistenceTest(
    @Autowired private val reactiveElasticsearchTestTemplate: ReactiveElasticsearchOperations,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @BeforeEach
    fun cleanup() {
        reactiveElasticsearchTestTemplate.delete(
            Query.findAll(), Board::class.java
        ).block()
    }

    @Test
    @DisplayName("Insert an entity and verify")
    fun insert_test() {
        val created: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val board1 = Board(
            boardId = "board1",
            title = "Hello, world!",
            content = "Java Hello World Tutorial",
            writer = "Writer0",
            like = 0,
            dislike = 0,
            created = created
        )

        StepVerifier
            .create(
                reactiveElasticsearchTestTemplate.save(
                    board1
                )
            )
            .expectNextMatches { savedEntity ->
                savedEntity.boardId ?. equals(
                    board1.boardId!!
                ) ?: false
            }
            .verifyComplete()

        StepVerifier
            .create(
                reactiveElasticsearchTestTemplate.search(
                    NativeSearchQueryBuilder()
                        .withQuery(QueryBuilders.matchQuery("boardId", "board1"))
                        .build(), Board::class.java
                )
            )
            .expectNextMatches { hit ->
                board1.isEquals(hit.content)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("Update an entity and verify")
    fun update_test() {
        val board1: Board = createDummy(
            "board1",
            "Hello, world!",
            "Java Hello World Tutorial",
            "Writer0",
            0,
            0
        )

        board1.writer = "Silk Sonic"
        StepVerifier
            .create(
                reactiveElasticsearchTestTemplate.save(board1)
            )
            .expectNextMatches { updatedEntity ->
                updatedEntity.writer == board1.writer
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("Delete an entity and verify")
    fun delete_test() {
        val board1: Board = createDummy(
            "board1",
            "Hello, world!",
            "Java Hello World Tutorial",
            "Writer0",
            0,
            0
        )

        StepVerifier
            .create(
                reactiveElasticsearchTestTemplate.delete(board1)
            )
            .expectNextMatches { removedEntityId ->
                removedEntityId == board1.boardId
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("Get by board id")
    fun get_by_board_id() {
        val board: Board = createDummy(
            "board1",
            "Hello, world!",
            "Java Hello World Tutorial",
            "Writer0",
            0,
            0
        )

        StepVerifier
            .create(
                reactiveElasticsearchTestTemplate.search(
                    NativeSearchQueryBuilder()
                        .withQuery(QueryBuilders.matchQuery("boardId", board.boardId))
                        .build(), Board::class.java
                )
            )
            .expectNextMatches { found ->
                board.isEquals(found.content)
            }
            .verifyComplete()
    }

    @Test
    @DisplayName("Paging and Sorting")
    fun paging_sorting_test() {
        val board1: Board = createDummy(
            "board1",
            "Hello, world!",
            "Java Hello World Tutorial",
            "Writer3",
            0,
            0
        )
        //  generate gap between entities
        //  to sort with a "created" field.
        Thread.sleep(100)
        val board2: Board = createDummy(
            "board2",
            "Hello, Rust!",
            "Basics of Rust programming language.",
            "Writer1",
            30,
            2
        )
        Thread.sleep(100)
        val board3: Board = createDummy(
            "board3",
            "Introduction to Kubernetes",
            "Practical codes about Docker and Kubernetes.",
            "Writer2",
            16,
            3
        )
        val rand = (Math.random() * 5).toInt()
        val fields = listOf("title", "writer", "created", "like", "dislike")

        val searchResult = reactiveElasticsearchTestTemplate
            .search(NativeSearchQueryBuilder()
                .withPageable(PageRequest.of(0, 1))
                .withSorts(
                    SortBuilders.fieldSort(fields[rand]).order(SortOrder.DESC)
                )
                .build(), Board::class.java)

        StepVerifier
            .create(
                searchResult
            )
            .expectNextMatches { hit ->
                when (fields[rand]) {
                    "title" -> board3.isEquals(hit.content)
                    "writer" -> board1.isEquals(hit.content)
                    "created" -> board3.isEquals(hit.content)
                    "like" -> board2.isEquals(hit.content)
                    "dislike" -> board3.isEquals(hit.content)
                    else -> false
                }
            }
            .verifyComplete()
    }

    private fun createDummy(
        boardId: String,
        title: String,
        content: String,
        writer: String,
        like: Long,
        dislike: Long,
    ): Board {
        val created: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val board = Board(
            boardId = boardId,
            title = title,
            content = content,
            writer = writer,
            like = like,
            dislike = dislike,
            created = created
        )

        //  save into the database
        reactiveElasticsearchTestTemplate.save(board)
            .block()
        return board
    }
    
    fun Board.isEquals(b: Board): Boolean 
    = this.boardId.equals(b.boardId)
            && this.title == b.title 
            && this.content == b.content 
            && this.writer == b.writer 
            && this.like == b.like 
            && this.dislike == b.dislike 
            && this.created == b.created 
            && this.comments.size == b.comments.size
}