package com.api.ealsticsearch.service.dto

import com.api.ealsticsearch.enums.ChainType
import com.api.ealsticsearch.enums.OrderType
import com.api.ealsticsearch.enums.StatusType
import java.math.BigDecimal

data class SaleResponse(
    val id : Long,
    val nftId : Long,
    val address: String,
    val createdDateTime: Long,
    val endDateTime: Long,
    val statusType: StatusType,
    val price: BigDecimal,
    val chainType: ChainType,
    val orderType: OrderType
)



