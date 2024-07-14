package com.example.tapochka

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), QuotesAdapter.OnItemClickListener, QuotesAdapter.OnItemLongClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QuotesAdapter
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)
        val stockApiKey = dbHelper.getApiKey("StockAPI")

        if (stockApiKey.isNullOrEmpty()) {
            Toast.makeText(this, "Пожалуйста, добавьте API ключ в настройках", Toast.LENGTH_LONG).show()
        }
        else {
            val addButton: FloatingActionButton = findViewById(R.id.floatingActionButton)
            addButton.setOnClickListener {
                showAddSymbolDialog()
            }
        }


        findViewById<FloatingActionButton>(R.id.analyzePortfolioButton).setOnClickListener {
            val intent = Intent(this, PortfolioAnalysisActivity::class.java)
            startActivity(intent)
        }

        val buttonSettings: FloatingActionButton = findViewById(R.id.button_settings)
        buttonSettings.setOnClickListener {
            val intent = Intent(this, ApiKeyActivity::class.java)
            startActivity(intent)
        }


        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = QuotesAdapter(emptyList(),this,this)
        recyclerView.adapter = adapter
        apiService = createApiService()

        loadQuotes()
    }

    override fun onItemClick(symbol: String) {
        val intent = Intent(this, CompanyProfileActivity::class.java).apply {
            putExtra("symbol", symbol)
        }
        lifecycleScope.launch {
            updateChartData(symbol)

            startActivity(intent)
        }
    }

    override fun onItemLongClick(symbol: String, action: String) {
        when (action) {
            "delete" -> {
                deleteSymbol(symbol)
                Log.e("onItemLongClick", "$symbol")

            }
            "edit" -> {

                showEditSymbolDialog(symbol)
                Log.e("onItemLongClick", "$symbol")
            }
        }
    }



    private fun loadQuotes() {
        val dbHelper = DatabaseHelper(this)
        val userSymbols = dbHelper.getUserSymbols()

        lifecycleScope.launch(Dispatchers.IO) {
            val stockApiKey = dbHelper.getApiKey("StockAPI")

            if (stockApiKey.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Пожалуйста, добавьте API ключ в настройках", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val quotes = userSymbols.flatMap { userSymbol ->
                try {
                    val quoteResponse = apiService.getPrice(userSymbol.symbol, stockApiKey)
                    quoteResponse?.map { quote ->
                        quote.count = userSymbol.count
                        // Сохраняем полученные данные о котировке в базу данных
                        dbHelper.insertOrUpdateQuote(quote)
                        quote
                    } ?: emptyList()
                } catch (e: Exception) {
                    Log.e("loadQuotes", "Error fetching quote for symbol: ${userSymbol.symbol}", e)
                    emptyList()
                }
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(quotes)
            }
        }
    }


    fun createApiService(): ApiService {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://fmpcloud.io/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ApiService::class.java)
    }
    private fun showAddSymbolDialog() {
        val dbHelper = DatabaseHelper(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Добавить акцию")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val inputSymbol = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Символ"
        }

        val inputCount = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Количество"
        }

        layout.addView(inputSymbol)
        layout.addView(inputCount)
        builder.setView(layout)
        builder.setPositiveButton("OK") { dialog, which ->

            val countStr = inputCount.text.toString().filter { it.isDigit() }
            val count = countStr.toIntOrNull() ?: 0
            val symbol = inputSymbol.text.toString().filter { it.isLetter() }.toUpperCase(Locale.getDefault())
            val insertSuccess = dbHelper.insertUserSymbol(symbol, count)

            if (!insertSuccess) {
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("This symbol already exists.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                fetchAndStoreCompanyProfile(symbol)
                loadQuotes()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    private fun fetchAndStoreCompanyProfile(symbol: String) {
        val dbHelper = DatabaseHelper(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val stockApiKey = dbHelper.getApiKey("StockAPI")

            if (stockApiKey.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Пожалуйста, добавьте API ключ в настройках", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            try {
                val response = apiService.getCompanyProfile(symbol, stockApiKey).awaitResponse()
                if (response.isSuccessful) {
                    response.body()?.let { profiles ->
                        profiles.forEach { profile ->
                            dbHelper.insertCompanyProfile(profile)
                        }
                    }
                } else {
                    Log.e("fetchAndStoreCompanyProfile", "Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("fetchAndStoreCompanyProfile", "Exception: ${e.message}", e)
            }
        }
    }


    fun updateChartData(symbol: String) {
        val dbHelper = DatabaseHelper(this)
        val stockApiKey = dbHelper.getApiKey("StockAPI")

        if (stockApiKey.isNullOrEmpty()) {
            Log.e("updateChartData", "API ключ отсутствует")
            runOnUiThread {
                Toast.makeText(this, "Пожалуйста, добавьте API ключ в настройках", Toast.LENGTH_LONG).show()
            }
            return
        }

        val calendar = Calendar.getInstance()
        val currentDate = calendar.time

        calendar.add(Calendar.YEAR, -4)
        val fromDate = calendar.time
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fromDateString = dateFormatter.format(fromDate)
        val toDateString = dateFormatter.format(currentDate)

        val call: Call<ChartDataResponse> = apiService.getChart(symbol, fromDateString, toDateString, stockApiKey)

        call.enqueue(object : Callback<ChartDataResponse> {
            override fun onResponse(call: Call<ChartDataResponse>, response: Response<ChartDataResponse>) {
                if (response.isSuccessful) {
                    val chartDataResponse = response.body()
                    chartDataResponse?.historical?.let { charts ->
                        dbHelper.insertCharts(symbol, charts)
                        Log.d("DatabaseDebug", "Data inserted for symbol: $symbol")
                    }
                } else {
                    Log.e("APIError", "Error fetching chart data for symbol: $symbol")
                }
            }

            override fun onFailure(call: Call<ChartDataResponse>, t: Throwable) {
                Log.e("APIFailure", "Failed to fetch chart data for symbol: $symbol, error: ${t.message}")
            }
        })
    }


    private fun deleteSymbol(symbol: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DatabaseHelper(this@MainActivity)
            dbHelper.deleteSymbol(symbol)
            withContext(Dispatchers.Main) {
                loadQuotes()
            }
        }
    }

    private fun showEditSymbolDialog(symbol: String) {
        val dbHelper = DatabaseHelper(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Symbol Count")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val inputCount = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter new count"
        }

        layout.addView(inputCount)
        builder.setView(layout)

        builder.setPositiveButton("OK") { dialog, which ->
            val newCount = inputCount.text.toString().toIntOrNull() ?: return@setPositiveButton
            updateSymbolCount(dbHelper, symbol, newCount)
            loadQuotes() // Обновляем список котировок после изменения
        }

        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        builder.show()
    }

    fun updateSymbolCount(dbHelper: DatabaseHelper, symbol: String, newCount: Int) {
        val db = dbHelper.writableDatabase
        val symbolLowerCase = symbol.lowercase() // Преобразование символа в нижний регистр
        val contentValues = ContentValues().apply {
            put(DbConstants.COLUMN_COUNT, newCount)
        }
        db.update(
            DbConstants.TABLE_USER_SYMBOLS,
            contentValues,
            "LOWER(${DbConstants.COLUMN_SYMBOL}) = ?",
            arrayOf(symbolLowerCase)
        )
    }
}