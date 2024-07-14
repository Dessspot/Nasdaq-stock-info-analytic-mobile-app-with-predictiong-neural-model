package com.example.tapochka

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.math3.filter.DefaultMeasurementModel
import org.apache.commons.math3.filter.DefaultProcessModel
import org.apache.commons.math3.filter.KalmanFilter
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Analytic : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dbHelper = DatabaseHelper(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytic)
        val symbol = intent.getStringExtra("symbol") ?: return
        displayAnalytics(symbol)

        val datasetBefore = dbHelper.getLast60ChartData(symbol)
        Log.d("DatasetDebug", "Dataset before applying filters: $datasetBefore")

        val dataset = applyFiltersToDataset(datasetBefore)
        Log.d("DatasetDebug", "Dataset after applying filters: $dataset")

        val model = MyModel(this)

        val forecastButton = findViewById<Button>(R.id.forecastButton)
        val forecastValue = findViewById<TextView>(R.id.forecastValue)

        forecastButton.setOnClickListener {
            try {
                val inputBuffer = prepareInput(dataset)
                val result = model.predict(inputBuffer)
                val prediction = result[0][0] // Предполагаем, что result это Array<FloatArray>
                val message = when {
                    prediction > 0.55 -> "Покупка($prediction)"
                    prediction in 0.45..0.55 -> "Нейтрально($prediction)"
                    prediction < 0.45 -> "Продажа($prediction)"
                    else -> "Ошибка: неверное значение прогноза"
                }
                val color = when {
                    prediction > 0.55 -> getColor(R.color.green) // Используем ресурсы
                    prediction in 0.45..0.55 -> getColor(R.color.gray)
                    prediction < 0.45 -> getColor(R.color.red)
                    else -> getColor(R.color.black) // Ошибка или недопустимое значение
                }

                forecastValue.text = message
                forecastValue.setTextColor(color)
            } catch (e: Exception) {
                Log.e("MyModel", "Error in prediction", e)
                forecastValue.text = "Ошибка в прогнозе"
                forecastValue.setTextColor(getColor(R.color.black))
            }
        }
    }

    private fun prepareInput(data: List<DataToForecast>): ByteBuffer {
        val numFeatures = 5 // количество признаков
        val buffer = ByteBuffer.allocateDirect(data.size * numFeatures * 4) // Float использует 4 байта
        buffer.order(ByteOrder.nativeOrder())

        for (item in data) {
            buffer.putFloat(item.close.toFloat())
            buffer.putFloat(item.high.toFloat())
            buffer.putFloat(item.low.toFloat())
            buffer.putFloat(item.open.toFloat())
            buffer.putFloat(item.volume.toFloat())
        }
        buffer.rewind() // Переместить курсор в начало буфера
        Log.d("MyModel", "Input data prepared for ${data.size} records.")
        return buffer
    }

    private fun applyFiltersToDataset(datasetBefore: List<DataToForecast>): List<DataToForecast> {
        val properties = arrayOf("close", "high", "low", "open", "volume")
        val dataset = mutableListOf<DataToForecast>()

        // Инициализация пустого списка для каждого свойства
        val columns = properties.associateWith { mutableListOf<Double>() }

        // Заполнение списков значений по каждому свойству
        datasetBefore.forEach { data ->
            columns["close"]!!.add(data.close)
            columns["high"]!!.add(data.high)
            columns["low"]!!.add(data.low)
            columns["open"]!!.add(data.open)
            columns["volume"]!!.add(data.volume.toDouble())
        }

        // Применение фильтра и масштабирования к каждому столбцу
        val filteredColumns = columns.mapValues { (property, values) ->
            val filtered = applyKalmanFilterToColumn(values.toDoubleArray())
            scaleColumnData(filtered)
        }

        // Пересоздание объектов DataToForecast с обработанными данными
        for (i in datasetBefore.indices) {
            val data = DataToForecast(
                close = filteredColumns["close"]!![i],
                high = filteredColumns["high"]!![i],
                low = filteredColumns["low"]!![i],
                open = filteredColumns["open"]!![i],
                volume = filteredColumns["volume"]!![i].toLong()
            )
            dataset.add(data)
        }

        return dataset
    }

    private fun scaleColumnData(columnData: DoubleArray): DoubleArray {
        if (columnData.isEmpty()) throw IllegalArgumentException("Input data column is empty")

        val min = columnData.minOrNull()!!
        val max = columnData.maxOrNull()!!
        val range = max - min

        // Проверка, чтобы избежать деления на ноль в случае, если все значения в столбце одинаковы
        return if (range == 0.0) {
            DoubleArray(columnData.size) { 1.0 } // Возвращаем массив из единиц, если все значения одинаковы
        } else {
            columnData.map { (it - min) / range }.toDoubleArray()
        }
    }

    fun applyKalmanFilterToColumn(data: DoubleArray): DoubleArray {
        // Предполагаемые начальные значения для фильтра
        val initialStateMean = data.average()
        val initialStateCovariance = 1.0
        val transitionCovariance = 0.01
        val observationCovariance = 0.01

        // Создание фильтра
        val kf = createKalmanFilter(initialStateMean, initialStateCovariance, transitionCovariance, observationCovariance)

        // Обработка данных через фильтр Калмана
        return data.map { datum ->
            kf.predict()
            kf.correct(doubleArrayOf(datum))
            kf.stateEstimation[0]
        }.toDoubleArray()
    }

    private fun createKalmanFilter(initialStateMean: Double, initialStateCovariance: Double, transitionCovariance: Double, observationCovariance: Double): KalmanFilter {
        val transitionMatrix = Array2DRowRealMatrix(arrayOf(doubleArrayOf(1.0))) // state transition matrix
        val observationMatrix = Array2DRowRealMatrix(arrayOf(doubleArrayOf(1.0))) // measurement matrix
        val processNoiseCovariance = Array2DRowRealMatrix(arrayOf(doubleArrayOf(transitionCovariance))) // process noise covariance matrix
        val measurementNoiseCovariance = Array2DRowRealMatrix(arrayOf(doubleArrayOf(observationCovariance))) // measurement noise covariance matrix
        val initialStateEstimate = ArrayRealVector(doubleArrayOf(initialStateMean)) // initial state estimation
        val initialErrorCovariance = Array2DRowRealMatrix(arrayOf(doubleArrayOf(initialStateCovariance))) // initial error covariance matrix

        val processModel = DefaultProcessModel(transitionMatrix, null, processNoiseCovariance, initialStateEstimate, initialErrorCovariance)
        val measurementModel = DefaultMeasurementModel(observationMatrix, measurementNoiseCovariance)
        return KalmanFilter(processModel, measurementModel)
    }

    private fun displayAnalytics(symbol: String) {
        val dbHelper = DatabaseHelper(this)

        val companyProfile = dbHelper.getCompanyProfile(symbol)

        // Calculate and get SMA and RSI
        val sma = dbHelper.calculateSMA(symbol, 30) // Example for a 30-day period
        val rsi = dbHelper.calculateRSI(symbol, 14) // Example for a 14-day period

        // Update UI
        findViewById<TextView>(R.id.companyName).text = companyProfile?.companyName ?: "N/A"

        val indicatorsContainer = findViewById<LinearLayout>(R.id.indicatorsContainer)
        indicatorsContainer.removeAllViews()

        addIndicatorToContainer(indicatorsContainer, "SMA", sma, determineSMASignal(sma))
        addIndicatorToContainer(indicatorsContainer, "RSI", rsi, determineRSISignal(rsi))

        val obvList = dbHelper.calculateOBV(symbol)
        val obv = obvList.lastOrNull()?.toDouble()
        addIndicatorToContainer(indicatorsContainer, "OBV", obv, determineOBVSignal(obv))

        val macdValues = dbHelper.calculateMACD(symbol)
        val macdLastValue = macdValues.first.lastOrNull()
        val signalLineLastValue = macdValues.second.lastOrNull()
        addIndicatorToContainer(indicatorsContainer, "MACD", macdLastValue, determineMACDSignal(macdLastValue, signalLineLastValue))

        val (smaList, upperBand, lowerBand) = dbHelper.calculateBollingerBands(symbol)
        val upperBandLastValue = upperBand.lastOrNull()
        val lowerBandLastValue = lowerBand.lastOrNull()
        addIndicatorToContainer(indicatorsContainer, "Bollinger Bands", upperBandLastValue, determineBollingerBandSignal(upperBandLastValue, lowerBandLastValue))

        val tradingVolumeAnalysis = dbHelper.analyzeTradingVolumes(symbol)
        val tradingVolumeAnalysisLastValue = tradingVolumeAnalysis.lastOrNull()
        addIndicatorToContainer(indicatorsContainer, "Trading Volume", tradingVolumeAnalysisLastValue?.first, determineTradingVolumeSignal(tradingVolumeAnalysisLastValue?.first))

        val daysToCover = dbHelper.calculateDaysToCover(symbol, totalShortPositions = 1000000) // Example totalShortPositions value
        addIndicatorToContainer(indicatorsContainer, "Days to Cover", daysToCover, determineDaysToCoverSignal(daysToCover))
    }

    private fun addIndicatorToContainer(container: LinearLayout, name: String, value: Any?, signal: String) {
        val indicatorValue = when (value) {
            is Double -> String.format("%.2f", value)
            is Int -> value.toString()
            is Long -> value.toString()
            else -> "N/A"
        }
        val valueText = "$name: $indicatorValue"
        setTextViewWithSignal(container, valueText, signal)
    }

    private fun setTextViewWithSignal(container: LinearLayout, value: String, signal: String) {
        val textView = TextView(this)
        textView.text = value
        textView.gravity = Gravity.START
        textView.setTextColor(getColor(R.color.black))

        val signalView = TextView(this)
        signalView.text = signal
        val color = when (signal) {
            "Покупка" -> getColor(R.color.green)
            "Нейтрально" -> getColor(R.color.gray)
            "Продажа" -> getColor(R.color.red)
            else -> getColor(R.color.black) // Ошибка или недопустимое значение
        }
        signalView.setTextColor(color)
        signalView.gravity = Gravity.END

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        linearLayout.addView(textView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        linearLayout.addView(signalView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        container.addView(linearLayout)
    }

    private fun determineSMASignal(sma: Double?): String {
        return when {
            sma == null -> "Ошибка"
            sma > 50 -> "Покупка"
            sma < 30 -> "Продажа"
            else -> "Нейтрально"
        }
    }

    private fun determineRSISignal(rsi: Double?): String {
        return when {
            rsi == null -> "Ошибка"
            rsi > 70 -> "Продажа"
            rsi < 30 -> "Покупка"
            else -> "Нейтрально"
        }
    }

    private fun determineOBVSignal(obv: Double?): String {
        return when {
            obv == null -> "Ошибка"
            obv > 1000000 -> "Покупка"
            obv < -1000000 -> "Продажа"
            else -> "Нейтрально"
        }
    }

    private fun determineMACDSignal(macd: Double?, signalLine: Double?): String {
        return when {
            macd == null || signalLine == null -> "Ошибка"
            macd > signalLine -> "Покупка"
            macd < signalLine -> "Продажа"
            else -> "Нейтрально"
        }
    }

    private fun determineBollingerBandSignal(upperBand: Double?, lowerBand: Double?): String {
        return when {
            upperBand == null || lowerBand == null -> "Ошибка"
            upperBand < 100 -> "Покупка"
            lowerBand > 100 -> "Продажа"
            else -> "Нейтрально"
        }
    }

    private fun determineTradingVolumeSignal(tradingVolume: Double?): String {
        return when {
            tradingVolume == null -> "Ошибка"
            tradingVolume > 1000000 -> "Покупка"
            tradingVolume < 100000 -> "Продажа"
            else -> "Нейтрально"
        }
    }

    private fun determineDaysToCoverSignal(daysToCover: Double?): String {
        return when {
            daysToCover == null -> "Ошибка"
            daysToCover < 2 -> "Покупка"
            daysToCover > 5 -> "Продажа"
            else -> "Нейтрально"
        }
    }
}
