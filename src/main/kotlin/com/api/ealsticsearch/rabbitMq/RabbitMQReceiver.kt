package com.api.ealsticsearch.rabbitMq

import com.api.ealsticsearch.service.ElasticsearchService
import com.api.ealsticsearch.service.dto.NftResponse
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class RabbitMQReceiver(
    private val elasticsearchService: ElasticsearchService,
) {
    @RabbitListener(bindings = [QueueBinding(
        value = Queue(name = "", durable = "false", exclusive = "true", autoDelete = "true"),
        exchange = Exchange(value = "nftExchange", type = ExchangeTypes.FANOUT)
    )])
    fun nftMessage(nft: NftResponse) {
        println("nft Response : " + nft.collectionName)
        elasticsearchService.save(nft)
            .subscribe()

    }
}