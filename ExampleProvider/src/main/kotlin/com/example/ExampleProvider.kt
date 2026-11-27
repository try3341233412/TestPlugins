package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "YouTube"
    override val hasMainPage = false
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)

    // Tried in order; the first one that answers is used
    private val instances = listOf(
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.nosebs.ru",
        "https://pipedapi-libre.kavin.rocks",
        "https://piped-api.privacy.com.de",
        "https://api.piped.yt",
        "https://pipedapi.drgns.space",
        "https://pipedapi.owo.si",
        "https://pipedapi.ducks.party",
        "https://piped-api.codespace.cz",
        "https://pipedapi.reallyaweso.me",
        "https://api.piped.private.coffee",
        "https://pipedapi.darkness.services",
        "https://pipedapi.kavin.rocks"
    )

    data class SearchResult(val items: List<Item>? = null)
    data class Item(
        val url: String? = null,
        val title: String? = null,
        val thumbnail: String? = null,
        val type: String? = null
    )
    data class StreamInfo(
        val title: String? = null,
        val description: String? = null,
        val thumbnailUrl: String? = null
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val idMatch = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})").find(query)
        if (idMatch != null) {
            val id = idMatch.groupValues[1]
            return listOf(
                newMovieSearchResponse("YouTube video", "$mainUrl/watch?v=$id", TvType.Movie) {
                    this.posterUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                }
            )
        }

        val q = URLEncoder.encode(query, "UTF-8")
        for (api in instances) {
            val items = try {
                app.get("$api/search?q=$q&filter=videos")
                    .parsedSafe<SearchResult>()?.items
            } catch (e: Exception) {
                null
            }
            if (items.isNullOrEmpty()) continue
            return items
                .filter { it.type == "stream" }
                .mapNotNull { item ->
                    val id = item.url?.substringAfter("v=", "").orEmpty()
                    if (id.isBlank()) return@mapNotNull null
                    newMovieSearchResponse(
                        item.title ?: "Video",
                        "$mainUrl/watch?v=$id",
                        TvType.Movie
                    ) { this.posterUrl = item.thumbnail }
                }
        }
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=")
        var info: StreamInfo? = null
        for (api in instances) {
            info = try {
                app.get("$api/streams/$id").parsedSafe<StreamInfo>()
            } catch (e: Exception) {
                null
            }
            if (info?.title != null) break
        }
        return newMovieLoadResponse(info?.title ?: "Video", url, TvType.Movie, url) {
            this.posterUrl = info?.thumbnailUrl
            this.plot = info?.description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return loadExtractor(data, subtitleCallback, callback)
    }
}
