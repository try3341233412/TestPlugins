package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class ExampleProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "YouTube"
    override val hasMainPage = false
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Others)

    // Public Piped API instance (can go down; swap if it does)
    private val api = "https://pipedapi.kavin.rocks"

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
        val q = URLEncoder.encode(query, "UTF-8")
        val res = app.get("$api/search?q=$q&filter=videos").parsedSafe<SearchResult>()
        return res?.items.orEmpty()
            .filter { it.type == "stream" }
            .mapNotNull { item ->
                val id = item.url?.substringAfter("v=", "").orEmpty()
                if (id.isBlank()) return@mapNotNull null
                newMovieSearchResponse(
                    item.title ?: "Video",
                    "$mainUrl/watch?v=$id",
                    TvType.Others
                ) { this.posterUrl = item.thumbnail }
            }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=")
        val info = app.get("$api/streams/$id").parsedSafe<StreamInfo>()
        return newMovieLoadResponse(info?.title ?: "Video", url, TvType.Others, url) {
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
