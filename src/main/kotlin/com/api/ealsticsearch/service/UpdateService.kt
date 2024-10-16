package com.api.ealsticsearch.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexResponse
import co.elastic.clients.elasticsearch.core.UpdateRequest
import co.elastic.clients.elasticsearch.core.UpdateResponse
import com.api.ealsticsearch.enums.StatusType
import com.api.ealsticsearch.service.dto.LedgerResponse
import com.api.ealsticsearch.service.dto.NftResponse
import com.api.ealsticsearch.service.dto.SaleResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class UpdateService(
    private val client: ElasticsearchClient,
    private val redisService: RedisService,
) {
    fun save(nft: NftResponse): Mono<IndexResponse> {
        return redisService.transformData(nft.id)
            .flatMap { updatedData ->
                client.index { idx ->
                    idx.index("nfts")
                        .id(updatedData.id.toString())
                        .document(updatedData)
                }.toMono()
            }
    }

    fun updatePrice(response: SaleResponse): Mono<UpdateResponse<Any>> {
        println(response.toString())
        return when(response.statusType){
            StatusType.ACTIVED -> update(response.price,response.nftId)
            StatusType.LEDGER, StatusType.CANCEL, StatusType.EXPIRED -> update(null,response.nftId)
            else -> throw Exception("")
        }
    }

    private fun update(price: BigDecimal?, nftId: Long) :Mono<UpdateResponse<Any>> {
        val roundedPrice = price?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
        val updateRequest = UpdateRequest.Builder<Any, Any>()
            .index("nfts")
            .id(nftId.toString())
            .doc(mapOf("lastPrice" to roundedPrice))
            .docAsUpsert(true)
            .build()

        return client.update(updateRequest, Any::class.java).toMono()
    }


    fun updateLedger(response: LedgerResponse) : Mono<UpdateResponse<Any>> {
        val price = response.ledgerPrice
        val roundedPrice = price.setScale(2, RoundingMode.HALF_UP)

        val updateRequest = UpdateRequest.Builder<Any, Any>()
            .index("nfts")
            .id(response.nftId.toString())
            .doc(mapOf(
                "ledgerPrice" to roundedPrice,
                "ledgerTime" to response.ledgerTime
            ))
            .docAsUpsert(true)
            .build()

        return client.update(updateRequest, Any::class.java).toMono()
    }
}