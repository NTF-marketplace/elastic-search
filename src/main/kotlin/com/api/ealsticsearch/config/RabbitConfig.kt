package com.api.ealsticsearch.config

import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {
    @Bean
    fun jsonMessageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        jsonMessageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = jsonMessageConverter
        return template
    }


    private fun createFanoutExchange(name: String): FanoutExchange {
        return FanoutExchange(name)
    }

    @Bean
    fun nftExchange() = createFanoutExchange("nftExchange")
}