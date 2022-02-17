package kr.dove.databases.elasticsearch.exceptions

class NotFoundException: Throwable {
    constructor(cause: Throwable): super(cause)
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
}