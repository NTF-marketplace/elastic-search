package com.api.ealsticsearch.kafka

import com.api.ealsticsearch.enums.OrderType
import com.api.ealsticsearch.enums.StatusType
import com.api.ealsticsearch.service.UpdateService
import com.api.ealsticsearch.service.dto.LedgerResponse
import com.api.ealsticsearch.service.dto.SaleResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Service

@Service
class KafkaConsumer(
    private val objectMapper: ObjectMapper,
    private val updateService: UpdateService,
){
    @KafkaListener(topics = ["ledgerResponse-topic"],
        groupId = "elastic-group",
        containerFactory = "kafkaListenerContainerFactory")
    fun consumeLedgerStatusEvents(message: Message<Any>) {
        val payload = message.payload

        if (payload is LinkedHashMap<*, *>) {
            val response = objectMapper.convertValue(payload, LedgerResponse::class.java)
            updateService.updateLedger(response).subscribe()
        }
    }

    @KafkaListener(topics = ["sale-topic"],
        groupId = "elastic-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumePriceEvents(message: Message<Any>) {
        val payload = message.payload

        if (payload is LinkedHashMap<*, *>) {
            val response = objectMapper.convertValue(payload, SaleResponse::class.java)
            println("response : " + response.toString())
            val availableStatus = listOf(StatusType.ACTIVED,StatusType.LEDGER, StatusType.EXPIRED, StatusType.CANCEL)
            if(response.orderType == OrderType.LISTING && response.statusType in availableStatus){
                updateService.updatePrice(response).subscribe()
            }
        }
    }



}