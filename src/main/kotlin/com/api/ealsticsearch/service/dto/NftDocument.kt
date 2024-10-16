package com.api.ealsticsearch.service.dto

import com.api.ealsticsearch.enums.ChainType
import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDateTime

data class NftDocument(
    val id: Long,
    val chainType: ChainType,
    val image: String,
    val collectionName: String,
    val collectionLogo: String?,
    val lastPrice: BigDecimal?,
    val ledgerTime: Long?,
    val ledgerPrice: BigDecimal?,
)
