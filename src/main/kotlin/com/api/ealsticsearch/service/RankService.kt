package com.api.ealsticsearch.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.JsonData
import com.api.ealsticsearch.enums.AGGREGATIONS_TYPE
import com.api.ealsticsearch.enums.ChainType
import com.api.ealsticsearch.service.dto.Document
import com.api.ealsticsearch.service.dto.RankDocument
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Service
class RankService(
    private val client: ElasticsearchClient,
) {
    fun extractAggregateValue(agg: Aggregate): Double {
        return when (agg._kind()) {
            Aggregate.Kind.Sum -> agg.sum().value()
            Aggregate.Kind.Min -> agg.min().value()
            Aggregate.Kind.Max -> agg.max().value()
            Aggregate.Kind.Avg -> agg.avg().value()
            Aggregate.Kind.ValueCount -> agg.valueCount().value()
            else -> {
                println("Unsupported aggregate kind: ${agg._kind()}")
                0.0
            }
        }
    }

    fun extractChainType(agg: Aggregate): ChainType? {
        return when (agg._kind()) {
            Aggregate.Kind.Sterms -> {
                val buckets = agg.sterms().buckets().array()
                if (buckets.isNotEmpty()) {
                    try {
                        ChainType.valueOf(buckets[0].key().stringValue().uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } else null
            }
            Aggregate.Kind.Lterms -> {
                val buckets = agg.lterms().buckets().array()
                if (buckets.isNotEmpty()) {
                    try {
                        ChainType.valueOf(buckets[0].key().toString().uppercase())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } else null
            }
            else -> {
                println("Unsupported chainType aggregate kind: ${agg._kind()}")
                null
            }
        }
    }


    fun updateRanking(type: AGGREGATIONS_TYPE, limit: Int = 10): Mono<MutableMap<AGGREGATIONS_TYPE, List<RankDocument>>> {
        return Mono.defer {
            val startTimeMillis = toTimestamp(type)
            println("Start time: $startTimeMillis")

            Mono.fromCallable {
                val response = client.search({ s: SearchRequest.Builder ->
                    s.index("nfts")
                        .size(0)
                        .query { q -> q.range { r ->
                            r.field("ledgerTime")
                                .gte(JsonData.of(startTimeMillis))
                        }}
                        .aggregations("by_collection") { a -> a.terms { t ->
                            t.field("collectionName.keyword")
                        }.aggregations("totalPrice") { sa -> sa.sum { sum ->
                            sum.field("ledgerPrice")
                        }}.aggregations("lowPrice") { sa -> sa.min { min ->
                            min.field("lastPrice")
                        }}.aggregations("highPrice") { sa -> sa.max { max ->
                            max.field("lastPrice")
                        }}.aggregations("chainType") { sa -> sa.terms { t ->
                            t.field("chainType.keyword")
                        }}}
                }, Nothing::class.java)

                val aggregations = response.aggregations()
                println("Aggregations: $aggregations")

                val byCollectionAgg = aggregations["by_collection"]?.sterms()
                println("byCollectionAgg: $byCollectionAgg")

                val rankings = byCollectionAgg?.buckets()?.array()?.mapNotNull { bucket ->
                    val collectionName = bucket.key().stringValue()
                    val totalPrice = extractAggregateValue(bucket.aggregations()["totalPrice"]!!)
                    val lowPrice = extractAggregateValue(bucket.aggregations()["lowPrice"]!!)
                    val highPrice = extractAggregateValue(bucket.aggregations()["highPrice"]!!)
                    val chainType = extractChainType(bucket.aggregations()["chainType"]!!)

                    println("Processing bucket: collectionName=$collectionName, totalPrice=$totalPrice, lowPrice=$lowPrice, highPrice=$highPrice, chainType=$chainType")

                    RankDocument(
                        collectionName = collectionName.toString(),
                        chainType = chainType!!,
                        totalPrice = totalPrice,
                        lowPrice = lowPrice,
                        highPrice = highPrice,
                        timeRange = type
                    )
                }?.sortedByDescending { it.totalPrice }?.take(limit) ?: emptyList()

                println("Final rankings: $rankings")
                mutableMapOf(type to rankings)
            }
        }
    }

    fun saveRankings(rankingsByTimeRange: MutableMap<AGGREGATIONS_TYPE, List<RankDocument>>): Mono<Void> {
        return Flux.fromIterable(rankingsByTimeRange.entries)
            .flatMap { (type, rankings) ->
                Flux.fromIterable(rankings)
                    .flatMap { ranking ->
                        Mono.fromCallable {
                            val response = client.index { indexRequest ->
                                indexRequest.index("rankings")
                                    .id("${ranking.collectionName}")
                                    .document(ranking)
                            }

                            if (response.result().name != "CREATED" && response.result().name != "UPDATED") {
                                println("Failed to save ranking for ${ranking.collectionName} in ${type.name} range")
                            }
                        }
                    }
            }
            .then()
    }

    // fun updateRanking(type: AGGREGATIONS_TYPE, limit: Int = 10): Mono<MutableMap<AGGREGATIONS_TYPE, List<RankDocument>>> {
    //     return Mono.defer {
    //         val startTimeMillis = toTimestamp(type)
    //
    //         val rangeQuery = RangeQuery.of { r ->
    //             r.field("ledgerTime")
    //                 .gte(JsonData.of(startTimeMillis))
    //         }
    //
    //         Mono.fromCallable {
    //             val searchResponse = client.search({ search ->
    //                 search.index("nfts")
    //                     .query { q -> q.range(rangeQuery) }
    //             }, Document::class.java)
    //
    //             val documents = searchResponse.hits().hits().mapNotNull { it.source() }
    //             val rankings = groupAndAggregateResults(documents, type, limit)
    //             mutableMapOf(type to rankings)
    //         }
    //     }
    // }
    //
    // private fun groupAndAggregateResults(
    //     documents: List<Document>,
    //     type: AGGREGATIONS_TYPE,
    //     limit: Int
    // ): List<RankDocument> {
    //     val groupedByCollection = documents.groupBy { it.collectionName to it.chainType }
    //
    //     return groupedByCollection.map { (key, docs) ->
    //         val (collectionName, chainType) = key
    //         val totalPrice = docs.sumOf { it.ledgerPrice ?: 0.0 }
    //         val lowPrice = docs.mapNotNull { it.lastPrice }
    //             .minOrNull() ?: 0.0
    //         val highPrice = docs.mapNotNull { it.lastPrice }
    //             .maxOrNull() ?: 0.0
    //
    //         RankDocument(
    //             collectionName = collectionName,
    //             chainType = chainType,
    //             totalPrice = totalPrice,
    //             lowPrice = lowPrice,
    //             highPrice = highPrice,
    //             timeRange = type
    //         )
    //     }.sortedByDescending { it.totalPrice }.take(limit)
    // }


    private fun toTimestamp(aggregationType: AGGREGATIONS_TYPE): Long {
        val now = Instant.now().toEpochMilli()  
        return when (aggregationType) {
            AGGREGATIONS_TYPE.ONE_HOURS -> now - Duration.ofHours(1).toMillis()
            AGGREGATIONS_TYPE.SIX_HOURS -> now - Duration.ofHours(6).toMillis()
            AGGREGATIONS_TYPE.ONE_DAY -> now - Duration.ofDays(1).toMillis()
            AGGREGATIONS_TYPE.SEVEN_DAY -> now - Duration.ofDays(7).toMillis()
        }
    }

}