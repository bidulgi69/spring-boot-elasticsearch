package kr.dove.databases.elasticsearch

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import javax.annotation.PreDestroy

abstract class ElasticsearchTestContainer {
    companion object {
        //  test 종료 이후,
        //  docker container 가 유지되는 경우를 방지.
        @PreDestroy
        fun stop() {
            CONTAINER.stop()
        }

        @Container
        @JvmStatic
        val CONTAINER = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.15.2")
            .apply { withEnv("discovery.type", "single-node") }
            .apply { addExposedPorts(9200) }
            .apply {
                waitingFor(
                    Wait.forHttp("/")
                        .forStatusCode(200)
                )
            }
            .apply { start() }
    }
}