package kr.dove.databases.elasticsearch.api

import kr.dove.databases.elasticsearch.persistence.Board
import kr.dove.databases.elasticsearch.persistence.Comment
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface BoardService {

    @PostMapping(
        value = ["/"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun post(@RequestBody board: Board): Mono<Board>

    @PostMapping(
        value = ["/comment/{boardId}"],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    fun comment(@RequestBody comment: Comment): Mono<Board>

    @GetMapping(
        value = ["/{boardId}"],
        produces = ["application/json"]
    )
    fun load(@PathVariable(name = "boardId") boardId: String): Mono<Board>

    @GetMapping(
        value = ["/all/{page}/{size}"],
        produces = ["application/x-ndjson"]
    )
    fun list(@PathVariable(name = "page") page: Int,
             @PathVariable(name = "size") size: Int): Flux<Board>

    @DeleteMapping(
        value = ["/{boardId}"],
        produces = ["application/json"]
    )
    fun delete(@PathVariable(name = "boardId") boardId: String): Mono<Board>
}