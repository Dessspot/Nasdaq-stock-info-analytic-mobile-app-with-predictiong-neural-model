package com.example.tapochka
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.appcompat.app.AlertDialog


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DbConstants.DATABASE_NAME, null, DbConstants.DATABASE_VERSION) {
    companion object {

        private const val TABLE_API_KEYS = "api_keys"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_KEY = "key"
    }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DbConstants.CREATE_TABLE_QUOTES)
        db.execSQL(DbConstants.CREATE_TABLE_PROFILES)
        db.execSQL(DbConstants.CREATE_TABLE_CHARTS)
        db.execSQL(DbConstants.CREATE_TABLE_USER_SYMBOLS)
        val createTable = "CREATE TABLE $TABLE_API_KEYS ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT, $COLUMN_KEY TEXT)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${DbConstants.TABLE_QUOTES}")
        db.execSQL("DROP TABLE IF EXISTS ${DbConstants.TABLE_PROFILES}")
        db.execSQL("DROP TABLE IF EXISTS ${DbConstants.TABLE_CHARTS}")
        db.execSQL("DROP TABLE IF EXISTS ${DbConstants.TABLE_USER_SYMBOLS}")

        db.execSQL("DROP TABLE IF EXISTS $TABLE_API_KEYS")

        onCreate(db)
    }


    fun clearDatabase() {
        val db = writableDatabase
        db.execSQL("DELETE FROM ${DbConstants.TABLE_QUOTES}")
        db.execSQL("DELETE FROM ${DbConstants.TABLE_PROFILES}")
        db.execSQL("DELETE FROM ${DbConstants.TABLE_CHARTS}")
        db.execSQL("DELETE FROM ${DbConstants.TABLE_USER_SYMBOLS}")
    }

    fun insertOrUpdateQuote(quote: Quote) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("symbol", quote.symbol)
            put("name", quote.name)
            put("price", quote.price)
            put("changesPercentage", quote.changesPercentage)
            put("change", quote.change)
            put("dayLow", quote.dayLow)
            put("dayHigh", quote.dayHigh)
            put("yearHigh", quote.yearHigh)
            put("yearLow", quote.yearLow)
            put("marketCap", quote.marketCap)
            put("priceAvg50", quote.priceAvg50)
            put("priceAvg200", quote.priceAvg200)
            put("exchange", quote.exchange)
            put("volume", quote.volume)
            put("avgVolume", quote.avgVolume)
            put("open", quote.open)
            put("previousClose", quote.previousClose)
            put("eps", quote.eps)
            put("pe", quote.pe)
            put("earningsAnnouncement", quote.earningsAnnouncement)
            put("sharesOutstanding", quote.sharesOutstanding)
            put("timestamp", quote.timestamp)
            put("count", quote.count)
        }

        val rowsAffected = db.update(DbConstants.TABLE_QUOTES, contentValues, "symbol = ?", arrayOf(quote.symbol))

        if (rowsAffected == 0) {
            db.insert(DbConstants.TABLE_QUOTES, null, contentValues)
        }
    }



    fun getUserSymbols(): List<UserSymbol> {
        val symbolsList = mutableListOf<UserSymbol>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT symbol, count FROM UserSymbol", null)

        if (cursor.moveToFirst()) {
            do {
                val symbol = cursor.getString(cursor.getColumnIndexOrThrow(DbConstants.COLUMN_SYMBOL))
                val count = cursor.getInt(cursor.getColumnIndexOrThrow("count"))
                symbolsList.add(UserSymbol(symbol, count))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return symbolsList
    }

    fun insertUserSymbol(symbol: String, count: Int): Boolean {
        val db = this.writableDatabase

        val query = "SELECT COUNT(*) FROM ${DbConstants.TABLE_USER_SYMBOLS} WHERE symbol = ?"
        val cursor = db.rawQuery(query, arrayOf(symbol))
        var symbolExists = false
        if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            if (count > 0) {
                symbolExists = true
            }
        }
        cursor.close()

        if (symbolExists) {
            db.close()
            return false
        }

        val contentValues = ContentValues().apply {
            put("symbol", symbol)
            put("count", count)
        }
        db.insertWithOnConflict(DbConstants.TABLE_USER_SYMBOLS, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
        return true
    }



    fun insertCompanyProfile(profile: CompanyProfile) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put("symbol", profile.symbol)
            put("price", profile.price)
            put("beta", profile.beta)
            put("volAvg", profile.volAvg)
            put("mktCap", profile.mktCap)
            put("lastDiv", profile.lastDiv)
            put("range", profile.range)
            put("changes", profile.changes)
            put("companyName", profile.companyName)
            put("currency", profile.currency)
            put("cik", profile.cik)
            put("isin", profile.isin)
            put("cusip", profile.cusip)
            put("exchange", profile.exchange)
            put("exchangeShortName", profile.exchangeShortName)
            put("industry", profile.industry)
            put("website", profile.website)
            put("description", profile.description)
            put("ceo", profile.ceo)
            put("sector", profile.sector)
            put("country", profile.country)
            put("fullTimeEmployees", profile.fullTimeEmployees)
            put("phone", profile.phone)
            put("address", profile.address)
            put("city", profile.city)
            put("state", profile.state)
            put("zip", profile.zip)
            put("dcfDiff", profile.dcfDiff)
            put("dcf", profile.dcf)
            put("image", profile.image)
            put("ipoDate", profile.ipoDate)
            put("defaultImage", profile.defaultImage)
            put("isEtf", profile.isEtf)
            put("isActivelyTrading", profile.isActivelyTrading)
            put("isAdr", profile.isAdr)
            put("isFund", profile.isFund)
        }

        db.insert(DbConstants.TABLE_PROFILES, null, values)
        db.close()
    }
    fun getCompanyProfile(symbol: String): CompanyProfileToView? {
        val db = this.readableDatabase
        val cursor = db.query(
            DbConstants.TABLE_PROFILES,
            arrayOf(
                "symbol", "range", "companyName", "currency", "cik", "isin", "cusip",
                "exchange", "exchangeShortName", "industry", "website", "description",
                "ceo", "sector", "country", "fullTimeEmployees", "phone", "address",
                "city", "state", "zip", "ipoDate"
            ), // Только необходимые столбцы
            "symbol = ?",
            arrayOf(symbol),
            null,
            null,
            null
        )
        var companyProfileToView: CompanyProfileToView? = null
        if (cursor.moveToFirst()) {
            companyProfileToView = CompanyProfileToView(
                symbol = cursor.getString(cursor.getColumnIndexOrThrow("symbol")),
                range = cursor.getString(cursor.getColumnIndexOrThrow("range")),
                companyName = cursor.getString(cursor.getColumnIndexOrThrow("companyName")),
                currency = cursor.getString(cursor.getColumnIndexOrThrow("currency")),
                cik = cursor.getString(cursor.getColumnIndexOrThrow("cik")),
                isin = cursor.getString(cursor.getColumnIndexOrThrow("isin")),
                cusip = cursor.getString(cursor.getColumnIndexOrThrow("cusip")),
                exchange = cursor.getString(cursor.getColumnIndexOrThrow("exchange")),
                exchangeShortName = cursor.getString(cursor.getColumnIndexOrThrow("exchangeShortName")),
                industry = cursor.getString(cursor.getColumnIndexOrThrow("industry")),
                website = cursor.getString(cursor.getColumnIndexOrThrow("website")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                ceo = cursor.getString(cursor.getColumnIndexOrThrow("ceo")),
                sector = cursor.getString(cursor.getColumnIndexOrThrow("sector")),
                country = cursor.getString(cursor.getColumnIndexOrThrow("country")),
                fullTimeEmployees = cursor.getString(cursor.getColumnIndexOrThrow("fullTimeEmployees")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                address = cursor.getString(cursor.getColumnIndexOrThrow("address")),
                city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                zip = cursor.getString(cursor.getColumnIndexOrThrow("zip")),
                ipoDate = cursor.getString(cursor.getColumnIndexOrThrow("ipoDate"))
            )
        }
        cursor.close()
        db.close()
        return companyProfileToView
    }

    fun insertCharts(symbol: String, charts: List<CompanyChart>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("charts", "symbol = ?", arrayOf(symbol))

            charts.forEach { chart ->
                val values = ContentValues().apply {
                    put("symbol", symbol)
                    put("date", chart.date) // Assuming the date is already in the correct format
                    put("open", chart.open)
                    put("low", chart.low)
                    put("high", chart.high)
                    put("close", chart.close)
                    put("volume", chart.volume)
                    put("adjClose", chart.adjClose)
                    put("unadjustedVolume", chart.unadjustedVolume)
                    put("change", chart.change)
                    put("changePercent", chart.changePercent)
                    put("vwap", chart.vwap)
                    put("label", chart.label)
                    put("changeOverTime", chart.changeOverTime)
                }
                db.insert("charts", null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteSymbol(symbol: String) {
        val db = writableDatabase
        val symbolLowerCase = symbol.lowercase() // Преобразование символа в нижний регистр
        db.beginTransaction()
        try {
            // Удаление из таблицы котировок
            db.delete(DbConstants.TABLE_QUOTES, "LOWER(symbol) = ?", arrayOf(symbolLowerCase))
            // Удаление из таблицы профилей компаний
            db.delete(DbConstants.TABLE_PROFILES, "LOWER(symbol) = ?", arrayOf(symbolLowerCase))
            // Удаление из таблицы графиков
            db.delete(DbConstants.TABLE_CHARTS, "LOWER(symbol) = ?", arrayOf(symbolLowerCase))
            // Удаление из таблицы пользовательских символов
            db.delete(DbConstants.TABLE_USER_SYMBOLS, "LOWER(symbol) = ?", arrayOf(symbolLowerCase))

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting symbol: $symbol", e)
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun calculateSMA(symbol: String, period: Int): Double {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT AVG(close) FROM (
            SELECT close FROM charts 
            WHERE symbol = ? 
            ORDER BY date DESC 
            LIMIT ?
        )
    """, arrayOf(symbol, period.toString()))

        var sma = 0.0
        if (cursor.moveToFirst()) {
            sma = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return sma
    }

    fun calculateRSI(symbol: String, period: Int): Double {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT close FROM charts 
        WHERE symbol = ? 
        ORDER BY date DESC 
        LIMIT ?
    """, arrayOf(symbol, (period + 1).toString()))

        val closes = mutableListOf<Double>()
        while (cursor.moveToNext()) {
            closes.add(cursor.getDouble(0))
        }
        cursor.close()
        db.close()

        if (closes.size <= period) return 0.0

        var gain = 0.0
        var loss = 0.0
        for (i in 1 until closes.size) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) {
                gain += change
            } else {
                loss -= change
            }
        }

        gain /= period
        loss /= period

        val rs = if (loss == 0.0) 0.0 else gain / loss
        return 100 - (100 / (1 + rs))
    }

    fun calculateOBV(symbol: String): List<Long> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT close, volume FROM charts 
        WHERE symbol = ? 
        ORDER BY date ASC
    """, arrayOf(symbol))

        val obvList = mutableListOf<Long>()
        var obv = 0L
        var previousClose = 0.0

        while (cursor.moveToNext()) {
            val close = cursor.getDouble(0)
            val volume = cursor.getLong(1)

            if (previousClose != 0.0) {
                if (close > previousClose) {
                    obv += volume
                } else if (close < previousClose) {
                    obv -= volume
                }
            }

            obvList.add(obv)
            previousClose = close
        }

        cursor.close()
        db.close()
        return obvList
    }


    fun calculateMACD(symbol: String): Pair<List<Double>, List<Double>> {
        val shortPeriod = 12
        val longPeriod = 26
        val signalPeriod = 9

        val shortEma = calculateEMAList(symbol, shortPeriod)
        val longEma = calculateEMAList(symbol, longPeriod)

        val macdLine = mutableListOf<Double>()
        for (i in 0 until shortEma.size) {
            macdLine.add(shortEma[i] - longEma[i])
        }

        val signalLine = calculateEMAList(macdLine, signalPeriod)

        return Pair(macdLine, signalLine)
    }

    private fun calculateEMAList(symbol: String, period: Int): List<Double> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT close FROM charts 
        WHERE symbol = ? 
        ORDER BY date ASC
    """, arrayOf(symbol))

        val closes = mutableListOf<Double>()
        while (cursor.moveToNext()) {
            closes.add(cursor.getDouble(0))
        }
        cursor.close()
        db.close()

        return calculateEMAList(closes, period)
    }

    private fun calculateEMAList(values: List<Double>, period: Int): List<Double> {
        val emaList = mutableListOf<Double>()
        if (values.isEmpty()) return emaList

        val multiplier = 2.0 / (period + 1)
        var ema = values[0]
        emaList.add(ema)

        for (i in 1 until values.size) {
            ema = ((values[i] - ema) * multiplier) + ema
            emaList.add(ema)
        }

        return emaList
    }


        private fun calculateEMA(symbol: String, period: Int): List<Double> {
            val closingPrices = getClosingPrices(symbol)
            val emaList = mutableListOf<Double>()
            val k = 2.0 / (period + 1)

            if (closingPrices.isNotEmpty()) {
                emaList.add(closingPrices.first()) // Инициализация первым значением цены закрытия

                for (i in 1 until closingPrices.size) {
                    val ema = (closingPrices[i] - emaList.last()) * k + emaList.last()
                    emaList.add(ema)
                }
            }

            return emaList
        }

        fun getClosingPrices(symbol: String): List<Double> {
            val closingPrices = mutableListOf<Double>()
            val db = this.readableDatabase

            // Запрос для получения цен закрытия по символу акции, отсортированный по дате
            // Предполагается, что у вас есть таблица 'charts' с колонками 'symbol' и 'close'
            val selectQuery = "SELECT close FROM charts WHERE symbol = ? ORDER BY date ASC"
            val cursor = db.rawQuery(selectQuery, arrayOf(symbol))

            while (cursor.moveToNext()) {
                val closePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("close"))
                closingPrices.add(closePrice)
            }
            cursor.close()
            db.close()

            return closingPrices
        }




        private fun calculateSignalLine(symbol: String, period: Int): List<Double> {
            return calculateEMA(symbol, period)
        }


    fun calculateBollingerBands(symbol: String, period: Int = 20, stdDevMultiplier: Double = 2.0): Triple<List<Double>, List<Double>, List<Double>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT close FROM charts 
        WHERE symbol = ? 
        ORDER BY date ASC
    """, arrayOf(symbol))

        val closes = mutableListOf<Double>()
        while (cursor.moveToNext()) {
            closes.add(cursor.getDouble(0))
        }
        cursor.close()
        db.close()

        val smaList = mutableListOf<Double>()
        val upperBandList = mutableListOf<Double>()
        val lowerBandList = mutableListOf<Double>()

        for (i in 0 until closes.size) {
            if (i + 1 >= period) {
                val subset = closes.subList(i + 1 - period, i + 1)
                val sma = subset.average()
                val stdDev = Math.sqrt(subset.map { Math.pow(it - sma, 2.0) }.average())

                smaList.add(sma)
                upperBandList.add(sma + stdDevMultiplier * stdDev)
                lowerBandList.add(sma - stdDevMultiplier * stdDev)
            } else {
                smaList.add(0.0)
                upperBandList.add(0.0)
                lowerBandList.add(0.0)
            }
        }

        return Triple(smaList, upperBandList, lowerBandList)
    }


        private fun calculateSMAList(symbol: String, period: Int): List<Double> {
            val closingPrices = getClosingPrices(symbol)
            val smaList = mutableListOf<Double>()

            for (i in 0 until closingPrices.size - period + 1) {
                val periodSum = closingPrices.subList(i, i + period).sum()
                smaList.add(periodSum / period)
            }

            return smaList
        }


        private fun calculateStdDevList(symbol: String, period: Int): List<Double> {
            val closingPrices = getClosingPrices(symbol)
            val smaList = calculateSMAList(symbol, period)
            val stdDevList = mutableListOf<Double>()

            for (i in 0 until closingPrices.size - period + 1) {
                val periodPrices = closingPrices.subList(i, i + period)
                val sma = smaList[i]
                val variance = periodPrices.sumOf { Math.pow(it - sma, 2.0) } / period
                stdDevList.add(Math.sqrt(variance))
            }

            return stdDevList
        }

    fun analyzeTradingVolumes(symbol: String): List<Pair<Double, Double>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT volume FROM charts 
        WHERE symbol = ? 
        ORDER BY date ASC
    """, arrayOf(symbol))

        val volumes = mutableListOf<Long>()
        while (cursor.moveToNext()) {
            volumes.add(cursor.getLong(0))
        }
        cursor.close()
        db.close()

        val avgVolume = volumes.average()
        val volumeDeviation = Math.sqrt(volumes.map { Math.pow(it - avgVolume, 2.0) }.average())

        return volumes.map { Pair(it.toDouble(), (it - avgVolume) / volumeDeviation) }
    }



    fun calculateDaysToCover(symbol: String, totalShortPositions: Int): Double {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
        SELECT volume FROM charts 
        WHERE symbol = ? 
        ORDER BY date DESC
        LIMIT 30
    """, arrayOf(symbol))

        val volumes = mutableListOf<Long>()
        while (cursor.moveToNext()) {
            volumes.add(cursor.getLong(0))
        }
        cursor.close()
        db.close()

        if (volumes.isEmpty()) return 0.0

        val avgVolume = volumes.average()
        return totalShortPositions / avgVolume
    }

    fun getBeta(symbol: String): Double? {
        val db = this.readableDatabase
        val cursor = db.query(
            DbConstants.TABLE_PROFILES,
            arrayOf("beta"),
            "symbol = ?",
            arrayOf(symbol),
            null,
            null,
            null
        )

        var beta: Double? = null
        if (cursor.moveToFirst()) {
            beta = cursor.getDouble(cursor.getColumnIndexOrThrow("beta"))
        }
        cursor.close()
        Log.d("DatabaseHelper", "Fetched beta for symbol $symbol: $beta")

        return beta
    }
    fun getQuote(symbol: String): Quote? {
        val db = this.readableDatabase
        val cursor = db.query(
            DbConstants.TABLE_QUOTES,
            null, // Используем null, чтобы выбрать все колонки
            "symbol = ?",
            arrayOf(symbol),
            null, null, null
        )

        var quote: Quote? = null
        if (cursor.moveToFirst()) {
            quote = Quote(
                symbol = cursor.getString(cursor.getColumnIndexOrThrow("symbol")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                changesPercentage = cursor.getDouble(cursor.getColumnIndexOrThrow("changesPercentage")),
                change = cursor.getDouble(cursor.getColumnIndexOrThrow("change")),
                dayLow = cursor.getDouble(cursor.getColumnIndexOrThrow("dayLow")),
                dayHigh = cursor.getDouble(cursor.getColumnIndexOrThrow("dayHigh")),
                yearHigh = cursor.getDouble(cursor.getColumnIndexOrThrow("yearHigh")),
                yearLow = cursor.getDouble(cursor.getColumnIndexOrThrow("yearLow")),
                marketCap = cursor.getLong(cursor.getColumnIndexOrThrow("marketCap")),
                priceAvg50 = cursor.getDouble(cursor.getColumnIndexOrThrow("priceAvg50")),
                priceAvg200 = cursor.getDouble(cursor.getColumnIndexOrThrow("priceAvg200")),
                exchange = cursor.getString(cursor.getColumnIndexOrThrow("exchange")),
                volume = cursor.getLong(cursor.getColumnIndexOrThrow("volume")),
                avgVolume = cursor.getLong(cursor.getColumnIndexOrThrow("avgVolume")),
                open = cursor.getDouble(cursor.getColumnIndexOrThrow("open")),
                previousClose = cursor.getDouble(cursor.getColumnIndexOrThrow("previousClose")),
                eps = cursor.getDouble(cursor.getColumnIndexOrThrow("eps")),
                pe = cursor.getDouble(cursor.getColumnIndexOrThrow("pe")),
                earningsAnnouncement = cursor.getString(cursor.getColumnIndexOrThrow("earningsAnnouncement")),
                sharesOutstanding = cursor.getLong(cursor.getColumnIndexOrThrow("sharesOutstanding")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                count = cursor.getInt(cursor.getColumnIndexOrThrow("count"))
            )
        }
        cursor.close()
        db.close()
        return quote
    }
    fun getLast60ChartData(symbol: String): List<DataToForecast> {
        val db = this.readableDatabase
        val cursor = db.query(
            DbConstants.TABLE_CHARTS,
            arrayOf("close", "high", "low", "open", "volume"),
            "symbol = ?",
            arrayOf(symbol),
            null,
            null,
            "date DESC", // Упорядочиваем по убыванию даты, чтобы получить последние значения
            "60" // Ограничиваем результат до последних 60 значений
        )

        val DataToForecast = mutableListOf<DataToForecast>()
        while (cursor.moveToNext()) {
            val close = cursor.getDouble(cursor.getColumnIndexOrThrow("close"))
            val high = cursor.getDouble(cursor.getColumnIndexOrThrow("high"))
            val low = cursor.getDouble(cursor.getColumnIndexOrThrow("low"))
            val open = cursor.getDouble(cursor.getColumnIndexOrThrow("open"))
            val volume = cursor.getLong(cursor.getColumnIndexOrThrow("volume"))

            val chartData = DataToForecast(close, high, low, open, volume)
            DataToForecast.add(chartData)
        }
        cursor.close()
        db.close()

        return DataToForecast
    }

    fun addApiKey(name: String, key: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_KEY, key)
        }
        db.insert(TABLE_API_KEYS, null, values)
    }




    fun getApiKey(name: String): String? {
        val db = readableDatabase
        val cursor = db.query(TABLE_API_KEYS, arrayOf(COLUMN_KEY), "$COLUMN_NAME = ?", arrayOf(name), null, null, null)
        return if (cursor.moveToFirst()) {
            val key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY))
            cursor.close()
            key
        } else {
            cursor.close()
            null
        }
    }



}


object DbConstants {
    const val DATABASE_VERSION = 1
    const val DATABASE_NAME = "CompanyDatabase.db"


    const val TABLE_QUOTES = "quotes"
    const val TABLE_PROFILES = "profiles"
    const val TABLE_CHARTS = "charts"
    const val TABLE_USER_SYMBOLS = "UserSymbol"
    const val TABLE_GPT_RESPONSES = "GptResponses"

    const val COLUMN_RESPONSE = "response"
    const val COLUMN_CREATED_AT = "createdAt"
    const val COLUMN_SYMBOL = "symbol"
    const val COLUMN_COUNT = "count"


    const val CREATE_TABLE_USER_SYMBOLS = """
CREATE TABLE UserSymbol (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol TEXT NOT NULL UNIQUE,
    count INTEGER
)"""

    const val CREATE_TABLE_QUOTES = """
CREATE TABLE $TABLE_QUOTES (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol TEXT NOT NULL,
    name TEXT,
    price REAL,
    changesPercentage REAL,
    change REAL,
    dayLow REAL,
    dayHigh REAL,
    yearHigh REAL,
    yearLow REAL,
    marketCap REAL,
    priceAvg50 REAL,
    priceAvg200 REAL,
    exchange TEXT,
    volume INTEGER,
    avgVolume INTEGER,
    open REAL,
    previousClose REAL,
    eps REAL,
    pe REAL,
    earningsAnnouncement TEXT,
    sharesOutstanding INTEGER,
    timestamp INTEGER,
    count INTEGER
)"""



    const val CREATE_TABLE_PROFILES = """
    CREATE TABLE $TABLE_PROFILES (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        symbol TEXT NOT NULL,
        price REAL,
        beta REAL,
        volAvg INTEGER,
        mktCap INTEGER,
        lastDiv REAL,
        range TEXT,
        changes REAL,
        companyName TEXT,
        currency TEXT,
        cik TEXT,
        isin TEXT,
        cusip TEXT,
        exchange TEXT,
        exchangeShortName TEXT,
        industry TEXT,
        website TEXT,
        description TEXT,
        ceo TEXT,
        sector TEXT,
        country TEXT,
        fullTimeEmployees TEXT,
        phone TEXT,
        address TEXT,
        city TEXT,
        state TEXT,
        zip TEXT,
        dcfDiff REAL,
        dcf REAL,
        image TEXT,
        ipoDate TEXT,
        defaultImage INTEGER,
        isEtf INTEGER,
        isActivelyTrading INTEGER,
        isAdr INTEGER,
        isFund INTEGER
    )"""



    const val CREATE_TABLE_CHARTS = """
CREATE TABLE charts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol TEXT NOT NULL,
    date TEXT NOT NULL,
    open REAL,
    low REAL,
    high REAL,
    close REAL,
    volume INTEGER,
    adjClose REAL,
    unadjustedVolume INTEGER,
    change REAL,
    changePercent REAL,
    vwap REAL,
    label TEXT,
    changeOverTime REAL
)"""


}
