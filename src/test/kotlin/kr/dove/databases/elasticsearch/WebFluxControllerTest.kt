package kr.dove.databases.elasticsearch

import kr.dove.databases.elasticsearch.persistence.Board
import kr.dove.databases.elasticsearch.persistence.Comment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [ElasticsearchTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class WebFluxControllerTest(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val reactiveElasticsearchTestTemplate: ReactiveElasticsearchOperations,
) {

    @BeforeEach
    fun cleanup() {
        reactiveElasticsearchTestTemplate
            .delete(Query.findAll(), Board::class.java)
            .block()
    }

    @Test
    @DisplayName("-XPOST /board/")
    fun post_board() {
        val board = Board(
            title = "Hello, world!",
            content = "Java Hello World Tutorial",
            writer = "Writer0",
            like = 0,
            dislike = 0,
            created = 0
        )

        val insertResponse = webTestClient
            .post()
            .uri("/board/")
            .bodyValue(board)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(Board::class.java)
        insertResponse.value {
            Assertions.assertEquals(board.title, it.title)
            board.apply {
                this.boardId = it.boardId
            }
        }

        //  if an object has boardId value,
        //  call load() function internally.
        val loadResponse = webTestClient
            .post()
            .uri("/board/")
            .bodyValue(board)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(Board::class.java)
        loadResponse.value {
            Assertions.assertEquals(board.boardId, it.boardId)
        }
    }

    @Test
    @DisplayName("-XPOST /board/comment/{boardId}")
    fun post_comment() {
        //  init: 0 comment
        val board = Board(
            boardId = "board1",
            title = "Hello, world!",
            content = "Java Hello World Tutorial",
            writer = "Writer0",
            like = 0,
            dislike = 0,
            created = 0
        )

        reactiveElasticsearchTestTemplate.save(board)
            .block()

        val comment = Comment(
            boardId = board.boardId,
            writer = "Yep",
            password = "4321",
            content = "It helps me a lot!!",
            created = 0
        )

        val response = webTestClient
            .post()
            .uri("/board/comment/${board.boardId!!}")
            .bodyValue(comment)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(Board::class.java)

        response.value {
            Assertions.assertTrue(
                comment.isEquals(it.comments[0])
            )
        }
    }

    @Test
    @DisplayName("-XGET /board/{boardId}")
    fun get_board() {
        val board = Board(
            title = "Hello, world!",
            content = "Java Hello World Tutorial",
            writer = "Writer0",
            comments = mutableListOf(
                Comment(
                    boardId = "board1",
                    writer = "ㅇㅇ",
                    password = "4321",
                    content = "It helps me a lot!!",
                    created = 0
                ),
                Comment(
                    boardId = "board1",
                    writer = "ㅇㅇ",
                    password = "1234",
                    content = "Good for beginners...",
                    created = 0
                )
            ),
            like = 0,
            dislike = 0,
            created = 0
        )

        //  create
        webTestClient
            .post()
            .uri("/board/")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(board)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(Board::class.java)
            .value { savedEntity ->
                board.apply {
                    this.boardId = savedEntity.boardId
                }
            }

        val response = webTestClient
            .get()
            .uri("/board/${board.boardId!!}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(Board::class.java)

        response.value { foundEntity ->
            Assertions.assertTrue(
                board.isEquals(foundEntity)
            )
        }
    }

    @Test
    @DisplayName("-XDELETE /board/{boardId}")
    fun delete_test() {
        val board = Board(
            boardId = "board1",
            title = "",
            content = "",
            writer = "",
            like = 0,
            dislike = 0
        )

        reactiveElasticsearchTestTemplate
            .save(board)
            .block()

        Assertions.assertEquals(
            1, reactiveElasticsearchTestTemplate
                .count(Board::class.java)
                .block()
                ?: 0L
        )

        //  delete
        val deleted = webTestClient
            .delete()
            .uri("/board/${board.boardId!!}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(String::class.java)

        deleted.value {
            Assertions.assertEquals(board.boardId, it)
        }

        Assertions.assertEquals(0, reactiveElasticsearchTestTemplate
            .count(Board::class.java)
            .block()
            ?: -1L)
    }

    @Test
    @DisplayName("-XGET /all/{page}/{size}")
    fun paging_test() {
        val board1 = Board(
            boardId = "board1",
            title = "",
            content = "",
            writer = "",
            like = 0,
            dislike = 0
        )
        val board2 = Board(
            boardId = "board2",
            title = "",
            content = "",
            writer = "",
            like = 0,
            dislike = 0
        )
        val board3 = Board(
            boardId = "board3",
            title = "",
            content = "",
            writer = "",
            like = 0,
            dislike = 0
        )

        reactiveElasticsearchTestTemplate
            .saveAll(listOf(board1, board2, board3), Board::class.java)
            .subscribe()

        val page = 0
        val size = 2
        val boardIds: List<String> = listOf(board1.boardId!!, board2.boardId!!, board3.boardId!!)
        webTestClient
            .get()
            .uri("/board/all/$page/$size")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBodyList(Board::class.java)
            .value<WebTestClient.ListBodySpec<Board>> { boards ->
                Assertions.assertEquals(2, boards.size)

                val ids = boards.map { b -> b.boardId!! }
                for (id: String in ids)
                    Assertions.assertTrue(
                        boardIds.contains(id)
                    )
            }
    }

    fun Board.isEquals(b: Board): Boolean {
        var equals = true
        for (i: Int in b.comments.indices) {
            if (equals) equals = this.comments[i].isEquals(b.comments[i])
            else break
        }
        return equals && (
                this.boardId.equals(b.boardId)
                        && this.title == b.title
                        && this.content == b.content
                        && this.writer == b.writer
                        && this.like == b.like
                        && this.dislike == b.dislike
                        && this.created == b.created
                        && this.comments.size == b.comments.size
                )
    }

    fun Comment.isEquals(comment: Comment): Boolean
    = this.boardId == comment.boardId
            && this.writer == comment.writer
            && this.content == comment.content
            && this.password == comment.password
}