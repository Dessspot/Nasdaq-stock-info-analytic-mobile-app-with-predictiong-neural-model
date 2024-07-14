package com.example.tapochka

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.formatter.ValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CompanyProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.company_profile)
        val symbol = intent.getStringExtra("symbol")
        fetchArticles((symbol.toString()))
        if (symbol != null) {
            val dbHelper = DatabaseHelper(this)
            val companyProfile = dbHelper.getCompanyProfile(symbol)
            companyProfile?.let {
                findViewById<TextView>(R.id.tvCompanyName).text = it.companyName
                findViewById<TextView>(R.id.tvSymbol).text = "Symbol: ${it.symbol}"
                findViewById<TextView>(R.id.tvRange).text = "Range: ${it.range}"
                findViewById<TextView>(R.id.tvCurrency).text = "Currency: ${it.currency}"
                findViewById<TextView>(R.id.tvCik).text = "CIK: ${it.cik}"
                findViewById<TextView>(R.id.tvIsin).text = "ISIN: ${it.isin}"
                findViewById<TextView>(R.id.tvCusip).text = "CUSIP: ${it.cusip}"
                findViewById<TextView>(R.id.tvExchange).text = "Exchange: ${it.exchange}"
                findViewById<TextView>(R.id.tvExchangeShortName).text = "Exchange Short Name: ${it.exchangeShortName}"
                findViewById<TextView>(R.id.tvIndustry).text = "Industry: ${it.industry}"
                findViewById<TextView>(R.id.tvWebsite).text = "Website: ${it.website}"
                findViewById<TextView>(R.id.tvDescription).text = "Description: ${it.description}"
                findViewById<TextView>(R.id.tvCeo).text = "CEO: ${it.ceo}"
                findViewById<TextView>(R.id.tvSector).text = "Sector: ${it.sector}"
                findViewById<TextView>(R.id.tvCountry).text = "Country: ${it.country}"
                findViewById<TextView>(R.id.tvFullTimeEmployees).text = "Full-time Employees: ${it.fullTimeEmployees}"
                findViewById<TextView>(R.id.tvPhone).text = "Phone: ${it.phone}"
                findViewById<TextView>(R.id.tvAddress).text = "Address: ${it.address}"
                findViewById<TextView>(R.id.tvCity).text = "City: ${it.city}"
                findViewById<TextView>(R.id.tvState).text = "State: ${it.state}"
                findViewById<TextView>(R.id.tvZip).text = "ZIP: ${it.zip}"
                findViewById<TextView>(R.id.tvIpoDate).text = "IPO Date: ${it.ipoDate}"
            }

            val spinner_data: Spinner = findViewById(R.id.spinner_chart_data)
            val fields_data = arrayOf("close", "open", "high", "low", "volume")
            val adapter_data = ArrayAdapter(this, android.R.layout.simple_spinner_item, fields_data)
            adapter_data.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_data.adapter = adapter_data

            val spinner_date: Spinner = findViewById(R.id.spinner_chart_date)
            val fields_date = arrayOf("all", "month", "3 month", "1 year", "2 year", "3 year", "4 year", "")
            val adapter_date = ArrayAdapter(this, android.R.layout.simple_spinner_item, fields_date) // Исправлено на корректное имя переменной
            adapter_date.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_date.adapter = adapter_date // Исправлено на правильное присвоение адаптера к спиннеру

            findViewById<Button>(R.id.btnLaunchGptIntegration).setOnClickListener {
                // Создаем интент для запуска GptIntegrationActivity
                val intent = Intent(this, Analytic::class.java).apply {
                    // Передаем символ акции как строковый экстра параметр
                    putExtra("symbol", symbol)
                }
                // Запускаем новую активность
                startActivity(intent)
            }

            updateChart("close","all",symbol)

            spinner_data.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val selectedField = fields_data[position]
                    val selectedDateRange = spinner_date.selectedItem.toString()
                    updateChart(selectedField, selectedDateRange,symbol) // Обновите график
                }
                override fun onNothingSelected(parent: AdapterView<*>) { }
            }

            spinner_date.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val selectedField = spinner_data.selectedItem.toString()
                    val selectedDateRange = fields_date[position]
                    updateChart(selectedField, selectedDateRange,symbol) // Обновите график
                }
                override fun onNothingSelected(parent: AdapterView<*>) { }
            }
        } else {
            Log.e("CompanyProfileActivity", "Symbol was not passed to the activity")
        }
    }

    fun updateChart(selectedField: String, selectedDateRange: String, symbol: String) {
        Log.d("ChartData", "Update chart called with field: $selectedField, range: $selectedDateRange")

        val entries: ArrayList<Entry> = ArrayList()
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val column = when (selectedField) {
            "open" -> "open"
            "low" -> "low"
            "high" -> "high"
            "close" -> "close"
            "volume" -> "volume"
            else -> "close"
        }

        val dateFilter = when (selectedDateRange) {
            "month" -> "date >= date('now','-1 month')"
            "3 month" -> "date >= date('now','-3 month')"
            "1 year" -> "date >= date('now','-1 year')"
            "2 year" -> "date >= date('now','-2 year')"
            "3 year" -> "date >= date('now','-3 year')"
            "4 year" -> "date >= date('now','-4 year')"
            else -> "1=1"
        }

        val query = "SELECT date, $column FROM charts WHERE $dateFilter and symbol = '$symbol' ORDER BY date ASC"
        Log.d("ChartData", "SQL Query: $query")
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val value = cursor.getFloat(cursor.getColumnIndexOrThrow(column))
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                Log.d("ChartData", "Read date: $dateStr, value: $value")
                date?.let {
                    val xValue = it.time.toFloat()
                    entries.add(Entry(xValue, value))
                    Log.d("ChartData", "Added Entry: x=$xValue, y=$value")
                } ?: Log.e("ChartData", "Failed to parse date: $dateStr")
            } while (cursor.moveToNext())
        }
        cursor.close()

        val dataSet = LineDataSet(entries, "Dataset for $selectedField")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.RED
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        val chart: LineChart = findViewById(R.id.chart)

        chart.clear()
        chart.data = lineData
        chart.description.text = "Chart for $selectedField"
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val dateFormat = when (selectedDateRange) {
                    "month", "3 month" -> SimpleDateFormat("MM/dd", Locale.US)
                    "year", "2024", "2023", "2022", "2021" -> SimpleDateFormat("MMM yyyy", Locale.US)
                    else -> SimpleDateFormat("yyyy", Locale.US)
                }
                return dateFormat.format(Date(value.toLong()))
            }
        }
        chart.invalidate()  // Refresh the chart with new data
        Log.d("ChartData", "Chart updated")
    }

    fun fetchArticles(symbol: String) {
        val dbHelper = DatabaseHelper(this)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val newsApiService = retrofit.create(NewsApiService::class.java)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val NewsAPI  = dbHelper.getApiKey("NewsAPI")
        if (NewsAPI .isNullOrEmpty()) {
            Log.e("fetchAndStoreCompanyProfile", "API ключ отсутствует")
            return
        }

        newsApiService.getArticles(symbol, currentDate, "relevancy", NewsAPI.toString()).enqueue(object :
            Callback<ArticleResponse> {
            override fun onResponse(call: Call<ArticleResponse>, response: Response<ArticleResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val articles = response.body()!!.articles
                    Log.e("newsApiService", response.body()!!.articles.toString())
                    displayArticles(articles)
                } else {
                    Log.e("newsApiService", "Failed to fetch articles")
                }
            }
            override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                Log.e("newsApiService", "Error fetching articles", t)
            }
        })
    }

    fun displayArticles(articles: List<Article>) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewToNews)
        recyclerView.layoutManager = LinearLayoutManager(this) // Используйте контекст активности
        recyclerView.adapter = ArticlesAdapter(this, articles)
    }
}