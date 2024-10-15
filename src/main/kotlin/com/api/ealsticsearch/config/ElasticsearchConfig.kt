package com.api.ealsticsearch.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ElasticsearchConfig {
    @Bean
    fun elasticsearchClient(): ElasticsearchClient {

        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val restClient = RestClient.builder(
            HttpHost("localhost", 9200)
        ).build()

        val transport: ElasticsearchTransport = RestClientTransport(
            restClient, JacksonJsonpMapper(objectMapper)
        )

        return ElasticsearchClient(transport)
    }
}