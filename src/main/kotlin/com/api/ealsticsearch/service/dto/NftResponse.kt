package com.api.ealsticsearch.service.dto

import com.api.ealsticsearch.enums.ChainType

data class NftResponse(
    val id : Long,
    val tokenId: String,
    val tokenAddress: String,
    val chainType: ChainType,
    val collectionName: String,
)
