package com.api.ealsticsearch

import com.api.ealsticsearch.enums.AGGREGATIONS_TYPE
import com.api.ealsticsearch.service.RankService
import com.api.ealsticsearch.service.UpdateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class ElasticSearchTest(
    @Autowired private val updateService: UpdateService,
    @Autowired private val rankService: RankService
) {

    @Test
    fun test() {
        val list = rankService.updateRanking(AGGREGATIONS_TYPE.ONE_DAY).block()

        println("Result Map: $list")

        // 특정 AGGREGATIONS_TYPE 키의 값을 출력
        val oneDayRankings = list?.get(AGGREGATIONS_TYPE.ONE_DAY)
        println("ONE_DAY Rankings: $oneDayRankings")

    }


    @Test
    fun test1() {
        val list = rankService.updateRanking12(AGGREGATIONS_TYPE.SEVEN_DAY).block()

        println("Result Map: $list")

        // 특정 AGGREGATIONS_TYPE 키의 값을 출력
        val oneDayRankings = list?.get(AGGREGATIONS_TYPE.ONE_DAY)
        println("ONE_DAY Rankings: $oneDayRankings")

    }

}