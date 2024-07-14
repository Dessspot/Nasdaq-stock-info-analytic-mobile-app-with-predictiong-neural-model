package com.example.tapochka
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class PortfolioAnalysisActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portfolio_analysis)
        val dbHelper = DatabaseHelper(this)

        val PortfolioAnalysis = loadPortfolioAnalysis()
        displayPortfolioData(PortfolioAnalysis)
        displaySectorDistribution(dbHelper)
    }

    fun loadPortfolioAnalysis(): PortfolioAnalysis {
        val dbHelper = DatabaseHelper(this)

        val totalValue = calculateTotalPortfolioValue(dbHelper)
        val returnRate = getExpectedReturn(dbHelper)
        val diversification = calculateDiversification(dbHelper)
        val riskLevel = getPortfolioRiskLevel()

        return PortfolioAnalysis(
            totalValue = totalValue,
            diversification = diversification,
            riskLevel = riskLevel,
            returnRate = returnRate
        )
    }
    fun calculateDiversification(dbHelper: DatabaseHelper): String {
        // Получаем список всех акций пользователя
        val userSymbols = dbHelper.getUserSymbols()
        val sectorsWeight = mutableMapOf<String, Double>()
        var totalPortfolioValue = 0.0

        // Считаем общую стоимость портфеля и вес секторов
        userSymbols.forEach { userSymbol ->
            val quote = dbHelper.getQuote(userSymbol.symbol)
            val profile = dbHelper.getCompanyProfile(userSymbol.symbol)
            val totalValue = (quote?.price?: 0.00) * userSymbol.count
            totalPortfolioValue += totalValue

            sectorsWeight[profile?.sector?:"hz"] = sectorsWeight.getOrDefault(profile?.sector?:"hz", 0.0) + totalValue
        }

        // Преобразуем веса секторов в процентное соотношение от общей стоимости портфеля
        val sectorsPercentage = sectorsWeight.mapValues { (_, value) -> value / totalPortfolioValue * 100 }

        // Оцениваем диверсификацию на основе распределения процентов по секторам
        val diversificationScore = sectorsPercentage.values.count { it > 20 } // Считаем количество секторов, занимающих более 20% портфеля

        return when {
            diversificationScore == 0 -> "High" // Высокая диверсификация: ни один сектор не занимает более 20% портфеля
            diversificationScore in 1..2 -> "Medium" // Средняя диверсификация: 1-2 сектора занимают более 20%
            else -> "Low" // Низкая диверсификация: более 2 секторов занимают более 20%
        }
    }


    private fun displayPortfolioData(data: PortfolioAnalysis) {
        val totalValueView = findViewById<TextView>(R.id.totalValue)
        val diversificationView = findViewById<TextView>(R.id.diversification)
        val riskLevelView = findViewById<TextView>(R.id.riskLevel)
        val returnRateView = findViewById<TextView>(R.id.returnRate)


        totalValueView.text = getString(R.string.total_value_format, data.totalValue)
        diversificationView.text = getString(R.string.diversification_format, data.diversification)
        riskLevelView.text = getString(R.string.risk_level_format, data.riskLevel)
        returnRateView.text = getString(R.string.return_rate_format, data.returnRate)
    }

    fun calculateTotalPortfolioValue(dbHelper: DatabaseHelper): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT price, count FROM ${DbConstants.TABLE_QUOTES}", null)
        var totalValue = 0.0
        while (cursor.moveToNext()) {
            val price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"))
            val count = cursor.getInt(cursor.getColumnIndexOrThrow("count"))
            totalValue += price * count
        }
        cursor.close()
        return totalValue
    }
    fun getExpectedReturn(dbHelper: DatabaseHelper): Double {
        val db = dbHelper.readableDatabase
        var totalReturn = 0.0
        val totalValue = calculateTotalPortfolioValue(dbHelper)
        val cursor = db.rawQuery("SELECT eps, pe, price, count FROM ${DbConstants.TABLE_QUOTES}", null)
        while (cursor.moveToNext()) {
            val eps = cursor.getDouble(0)
            val pe = cursor.getDouble(1)
            val price = cursor.getDouble(2)
            val count = cursor.getInt(3)
            val currentValue = price * count
            // Примерная логика расчета, может быть адаптирована под ваши нужды
            val expectedPrice = eps * pe
            val expectedReturn = (expectedPrice - price) / price * 100 // Процентная доходность
            totalReturn += expectedReturn * (currentValue / totalValue)
        }
        cursor.close()
        return totalReturn
    }
    fun getPortfolioRiskLevel(): String {
        val dbHelper = DatabaseHelper(this)
        val userSymbols = dbHelper.getUserSymbols()
        if (userSymbols.isEmpty()) return "Неизвестно"

        var totalBeta = 0.0
        var totalShares = 0

        userSymbols.forEach { userSymbol ->
            val beta = dbHelper.getBeta(userSymbol.symbol)
            Log.d("getPortfolioRiskLevel", "Symbol: ${userSymbol.symbol}, Beta: $beta, Count: ${userSymbol.count}")
            if (beta == null) {
                Log.d("getPortfolioRiskLevel", "Beta is null for ${userSymbol.symbol}")
                return@forEach
            }
            totalBeta += beta * userSymbol.count
            totalShares += userSymbol.count
        }


        if (totalShares == 0) return "Неизвестно"

        val averageBeta = totalBeta / totalShares
        return calculateRiskLevel(averageBeta)
    }

    private fun calculateRiskLevel(beta: Double): String {
        return when {
            beta < 0.8 -> "Очень низкий"
            beta < 1 -> "Низкий"
            beta == 1.0 -> "Средний"
            beta <= 1.2 -> "Выше среднего"
            else -> "Высокий"
        }
    }
    fun calculateSectorDistribution(dbHelper: DatabaseHelper): Map<String, Double> {
        val sectorsDistribution = mutableMapOf<String, Double>()
        val userSymbols = dbHelper.getUserSymbols()
        var totalPortfolioValue = 0.0
        Log.d("SectorDistribution", "Sector")

        userSymbols.forEach { userSymbol ->
            val quote = dbHelper.getQuote(userSymbol.symbol)
            val profile = dbHelper.getCompanyProfile(userSymbol.symbol)
            if (quote != null && profile != null) {
                val totalValue = quote.price * userSymbol.count
                totalPortfolioValue += totalValue
                sectorsDistribution[profile.sector ?:"null"] = sectorsDistribution.getOrDefault(profile.sector, 0.0) + totalValue
            }
        }

        val sectorsPercentage = sectorsDistribution.mapValues { (key, value) -> value / totalPortfolioValue * 100 }
        sectorsPercentage.forEach { (sector, value) ->
            Log.d("SectorDistribution", "Sector: $sector, Value: $value")
        }
        return sectorsPercentage
    }
    private fun displaySectorDistribution(dbHelper: DatabaseHelper) {
        val sectorsDistribution = calculateSectorDistribution(dbHelper)
        val pieEntries: MutableList<PieEntry> = mutableListOf()




        if(pieEntries.isEmpty()) {
            Log.d("PieChart", "PieEntries is empty")
        } else {
            Log.d("PieChart", "PieEntries prepared, size: ${pieEntries.size}")
        }

        sectorsDistribution.forEach { (sector, value) ->
            pieEntries.add(PieEntry(value.toFloat(), sector))
        }

        val pieDataSet = PieDataSet(pieEntries, "Распределение по секторам")
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val pieData = PieData(pieDataSet)

        val pieChart = findViewById<PieChart>(R.id.sectorsPieChart)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = "Сектора"
        pieChart.animateY(1000, Easing.EaseInOutQuad)

        pieChart.invalidate()
    }





}


