package com.api.ealsticsearch.service.dto

import com.api.ealsticsearch.enums.ChainType
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Document (
    val collectionName: String,
    val chainType: ChainType,
    val ledgerPrice: Double?,
    val lastPrice: Double?
)

