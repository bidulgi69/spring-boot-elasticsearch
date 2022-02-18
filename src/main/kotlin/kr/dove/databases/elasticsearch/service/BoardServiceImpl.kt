package kr.dove.databases.elasticsearch.service

import kr.dove.databases.elasticsearch.api.BoardService
import kr.dove.databases.elasticsearch.exceptions.NotFoundException
import kr.dove.databases.elasticsearch.persistence.Board
import kr.dove.databases.elasticsearch.persistence.Comment
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.data.domain.PageRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping(value = ["/board"])
class BoardServiceImpl(
    private val reactiveElasticsearchTemplate: ReactiveElasticsearchOperations
): BoardService {
    override fun post(board: Board): Mono<Board> {
        return board.boardId ?. let { boardId ->
            this.load(boardId)
        } ?: run {
            reactiveElasticsearchTemplate.save(board.apply {
                this.boardId = UUID.randomUUID().toString()
            })
        }
    }

    override fun comment(boardId: String, comment: Comment): Mono<Board> {
        val queryBuilder: NativeSearchQueryBuilder = NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("boardId", boardId))
            )

        return reactiveElasticsearchTemplate.search(queryBuilder.build(), Board::class.java)
            .flatMap { hit -> Mono.just(hit.content) }
            .elementAt(0)
            .doOnError(IndexOutOfBoundsException::class.java) { throw NotFoundException("Invalid Board id.") }
            .flatMap { board ->
                reactiveElasticsearchTemplate.save(board.apply {
                    this.comments.add(
                        comment.apply {
                            this.boardId = boardId
                            this.created = Date().time
                        }
                    )
                })
            }
    }

    override fun load(boardId: String): Mono<Board> {
        return reactiveElasticsearchTemplate.search(NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("boardId", boardId)))
            .build(), Board::class.java)
            .flatMap { hit ->
                Mono.just(hit.content)
            }
            .elementAt(0)
            .onErrorResume { Mono.error(NotFoundException("Invalid Board id.")) }
    }

    override fun list(page: Int, size: Int): Flux<Board> {
        return reactiveElasticsearchTemplate.search(NativeSearchQueryBuilder()
            .withPageable(PageRequest.of(page, size))
            .build(), Board::class.java
        )
            .flatMap { hit ->
                Mono.just(hit.content)
            }
    }

    override fun delete(boardId: String): Mono<String> {
        return this.load(boardId)
            .flatMap { board ->
                reactiveElasticsearchTemplate.delete(board)
            }
    }
}