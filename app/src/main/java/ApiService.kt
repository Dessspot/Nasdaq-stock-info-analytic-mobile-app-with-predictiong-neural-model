package com.example.tapochka
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/v3/quote/{symbol}")
    suspend fun getPrice(
        @Path("symbol") symbol: String,
        @Query("apikey") token: String
    ): List<Quote>

    @GET("api/v3/profile/{symbol}")
    fun getCompanyProfile(
        @Path("symbol") symbol: String,
        @Query("apikey") token: String
    ): Call<List<CompanyProfile>>

    @GET("api/v3/historical-price-full/{symbol}")
    fun getChart(
        @Path("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("apikey") token: String
    ):  Call<ChartDataResponse>

    @POST("gpt/analyze")
    fun analyzeStockTimeSeries(
        @Body gptRequest: GptRequest
    ): Call<GptResponse>



}

interface NewsApiService {
    @GET("v2/everything")
    fun getArticles(
        @Query("q") query: String,
        @Query("from") from: String,
        @Query("sortBy") sortBy: String,
        @Query("apiKey") apiKey: String
    ): Call<ArticleResponse>
}

data class UserSymbol(
    val symbol: String,
    val count: Int
)

data class CompanyQuote(
    val name: String,
    val symbol: String,
    val price: Double,
    val changesPercentage: Double,
    var count: Int = 0
)

data class CompanyProfile(
    val symbol: String,
    val price: Double,
    val beta: Double,
    val volAvg: Long,
    val mktCap: Long?,
    val lastDiv: Double?,
    val range: String?,
    val changes: Double,
    val companyName: String,
    val currency: String?,
    val cik: String?,
    val isin: String?,
    val cusip: String?,
    val exchange: String?,
    val exchangeShortName: String?,
    val industry: String?,
    val website: String?,
    val description: String?,
    val ceo: String?,
    val sector: String?,
    val country: String?,
    val fullTimeEmployees: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val dcfDiff: Double?,
    val dcf: Double?,
    val image: String?,
    val ipoDate: String?,
    val defaultImage: Boolean?,
    val isEtf: Boolean?,
    val isActivelyTrading: Boolean?,
    val isAdr: Boolean?,
    val isFund: Boolean?
)

data class ChartDataResponse(
    val symbol: String,
    val historical: List<CompanyChart>
)

data class CompanyChart(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val adjClose: Double,
    val volume: Long,
    val unadjustedVolume: Long,
    val change: Double,
    val changePercent: Double,
    val vwap: Double,
    val label: String,
    val changeOverTime: Double
)


data class CompanyProfileToView(
    val symbol: String,
    val range: String?,
    val companyName: String?,
    val currency: String?,
    val cik: String?,
    val isin: String?,
    val cusip: String?,
    val exchange: String?,
    val exchangeShortName: String?,
    val industry: String?,
    val website: String?,
    val description: String?,
    val ceo: String?,
    val sector: String?,
    val country: String?,
    val fullTimeEmployees: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val ipoDate: String?
)

data class TimeSeriesData(
    val time_series: List<CompanyChart>
)

data class GptRequest(
    val prompt: String,
    val temperature: Double,
    val max_tokens: Int,
    val top_p: Double,
    val frequency_penalty: Double,
    val presence_penalty: Double,
    val user: String
)

data class GptResponse(
    val id: String,
    val objects: String,
    val created: Int,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val text: String,
    val index: Int,
    val logprobs: Any?,
    val finish_reason: String
)

data class ArticleResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val source: Source?,
    val author: String?,
    val title: String,
    val description: String,
    val url: String,
    val urlToImage: String?,
    val publishedAt: String,
    val content: String?
)


data class Source(
    val id: String?,
    val name: String?
)


data class NewsItem(
    val title: String,
    val description: String,
    val publishedAt: String,
    val content: String?
)
data class PortfolioAnalysis(
    val totalValue: Double,
    val diversification: String,
    val riskLevel: String,
    val returnRate: Double
)

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val changesPercentage: Double,
    val change: Double,
    val dayLow: Double,
    val dayHigh: Double,
    val yearHigh: Double,
    val yearLow: Double,
    val marketCap: Long,
    val priceAvg50: Double,
    val priceAvg200: Double,
    val exchange: String,
    val volume: Long,
    val avgVolume: Long,
    val open: Double,
    val previousClose: Double,
    val eps: Double,
    val pe: Double,
    val earningsAnnouncement: String,
    val sharesOutstanding: Long,
    val timestamp: Long,
    var count: Int = 0
)

