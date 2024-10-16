package com.api.ealsticsearch.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.JsonData
import com.api.ealsticsearch.service.dto.NftDocument
import com.api.ealsticsearch.service.dto.RankDocument
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SearchService(
    private val client: ElasticsearchClient,
) {


    fun searchNFTs(
        searchTerm: String,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        sortBy: String = "lastPrice",
        sortOrder: String = "desc",
        page: Int = 0,
        size: Int = 10
    ): Mono<List<NftDocument>> {
        return Mono.fromCallable {
            val searchRequest = SearchRequest.Builder()
                .index("nfts")
                .query { q ->
                    q.bool { b ->
                        b.must(
                            Query.of { q -> q.multiMatch { m ->
                                m.fields("name", "description", "collectionName")
                                    .query(searchTerm)
                                    .fuzziness("AUTO")
                            } }
                        )
                        if (minPrice != null || maxPrice != null) {
                            b.filter(
                                Query.of { q ->
                                    q.range { r->
                                        r.field("lastPrice")
                                        if (minPrice != null) r.gte(JsonData.of(minPrice))
                                        if (maxPrice != null) r.lte(JsonData.of(maxPrice))
                                        r
                                    }

                                }
                            )
                        }
                        b
                    }
                }
                .sort { s ->
                    s.field { f ->
                        f.field(sortBy)
                            .order(SortOrder.valueOf(sortOrder.uppercase()))
                    }
                }
                .from(page * size)
                .size(size)
                .build()

            val searchResponse = client.search(searchRequest, NftDocument::class.java)
            searchResponse.hits().hits().mapNotNull { it.source() }
        }
    }

    fun searchCollections(collectionName: String, page: Int = 0, size: Int = 10): Mono<List<RankDocument>> {
        return Mono.fromCallable {
            val searchRequest = SearchRequest.Builder()
                .index("rankings")
                .query { q ->
                    q.multiMatch { m ->
                        m.fields("collectionName")
                            .query(collectionName)
                            .fuzziness("AUTO")
                    }
                }
                .from(page * size)
                .size(size)
                .build()

            val searchResponse = client.search(searchRequest, RankDocument::class.java)
            searchResponse.hits().hits().mapNotNull { it.source() }
        }
    }


}