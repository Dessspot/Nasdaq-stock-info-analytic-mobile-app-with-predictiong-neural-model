package com.example.tapochka
import org.apache.commons.math3.filter.*
import org.apache.commons.math3.linear.*

data class DataToForecast(
    var close: Double,
    var high: Double,
    var low: Double,
    var open: Double,
    var volume: Long
) {
    fun applyKalmanFilter(): DataToForecast {
        val properties = arrayOf("close", "high", "low", "open", "volume")
        val filteredData = DataToForecast(close, high, low, open, volume)

        for (property in properties) {
            val dataToFilter = when (property) {
                "close" -> close
                "high" -> high
                "low" -> low
                "open" -> open
                "volume" -> volume.toDouble()
                else -> throw IllegalArgumentException("Unknown property: $property")
            }

            val initialStateMean = dataToFilter
            val initialStateCovariance = 1.0
            val transitionCovariance = 0.01
            val observationCovariance = 0.01

            val kf = createKalmanFilter(initialStateMean, initialStateCovariance, transitionCovariance, observationCovariance)

            kf.predict()
            kf.correct(doubleArrayOf(dataToFilter))

            val filteredValue = kf.stateEstimation[0]

            when (property) {
                "close" -> filteredData.close = filteredValue
                "high" -> filteredData.high = filteredValue
                "low" -> filteredData.low = filteredValue
                "open" -> filteredData.open = filteredValue
                "volume" -> filteredData.volume = filteredValue.toLong()
            }
        }

        return filteredData
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
}
