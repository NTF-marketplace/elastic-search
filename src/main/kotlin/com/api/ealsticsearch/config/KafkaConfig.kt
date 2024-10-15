package com.api.ealsticsearch.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.kafka.support.serializer.JsonDeserializer

@Configuration
@EnableKafka
class KafkaConfig {
    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun kafkaAdmin(): KafkaAdmin = KafkaAdmin(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers))

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "elastic-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java.name,
                JsonDeserializer.TRUSTED_PACKAGES to "*",
                JsonDeserializer.VALUE_DEFAULT_TYPE to Any::class.java.name,
            )
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), JsonDeserializer(Any::class.java, false))
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(4)
        factory.setCommonErrorHandler(
            object : CommonErrorHandler {
                override fun handleRemaining(
                    thrownException: Exception,
                    records: List<org.apache.kafka.clients.consumer.ConsumerRecord<*, *>>,
                    consumer: org.apache.kafka.clients.consumer.Consumer<*, *>,
                    container: MessageListenerContainer,
                ) {
                    logger.error("Error in consumer: ${thrownException.message}", thrownException)
                    logger.error("Problematic records: $records")
                }
            },
        )
        return factory
    }
}