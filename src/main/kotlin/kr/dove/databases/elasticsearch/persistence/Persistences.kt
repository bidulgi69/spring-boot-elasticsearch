package kr.dove.databases.elasticsearch.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Mapping
import org.springframework.data.elasticsearch.annotations.Setting
import org.springframework.data.elasticsearch.annotations.WriteTypeHint

@Document(
    indexName = "board",
    writeTypeHint = WriteTypeHint.FALSE
)
@Setting(settingPath = "/elasticsearch/settings/settings.json")
@Mapping(mappingPath = "/elasticsearch/mappings/mappings.json")
data class Board(
    @Id var boardId: String? = null,
    var title: String,
    var content: String,
    var writer: String,
    val password: String? = null,
    var comments: MutableList<Comment> = mutableListOf(),
    var like: Long,
    var dislike: Long,
    var created: Long? = null,
)

data class Comment(
    val boardId: String,
    val writer: String,
    val password: String,
    var content: String,
    val created: Long,
)