package com.api.ealsticsearch.service.dto

import com.api.ealsticsearch.enums.AGGREGATIONS_TYPE
import com.api.ealsticsearch.enums.ChainType

data class RankDocument(
    val collectionName: String,
    val chainType: ChainType,
    val totalPrice: Double? = 0.0,
    val lowPrice: Double? = 0.0,
    val highPrice: Double?= 0.0,
    val timeRange: AGGREGATIONS_TYPE
)
