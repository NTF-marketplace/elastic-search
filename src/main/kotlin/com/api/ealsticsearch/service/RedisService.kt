package com.api.ealsticsearch.service

import com.api.ealsticsearch.service.dto.NftDocument
import com.api.ealsticsearch.service.dto.NftMetadataResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class RedisService(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {
    fun transformData(nftId: Long): Mono<NftDocument> {
        return reactiveRedisTemplate.opsForValue().get("NFT:$nftId")
            .map { data ->
                val nftMetadata = objectMapper.convertValue(data, NftMetadataResponse::class.java)
                println("nftMetadata: " + nftMetadata.toString())
                NftDocument(
                    id = nftMetadata.id,
                    chainType = nftMetadata.chainType,
                    lastPrice = null,
                    collectionName = nftMetadata.collectionName,
                    collectionLogo = nftMetadata.collectionLogo,
                    ledgerTime = null,
                    ledgerPrice = null,
                )
            }
    }
}