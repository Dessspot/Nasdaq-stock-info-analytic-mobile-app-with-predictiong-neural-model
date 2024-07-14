package com.example.tapochka

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class MyModel(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        try {
            val model = loadModelFile(context, "model.tflite")
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        try {
            val assetFileDescriptor = context.assets.openFd(modelName)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            Log.d("MyModel", "Model file is found. Size: $declaredLength bytes")
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("MyModel", "Failed to load model $modelName", e)
            throw RuntimeException("Error loading model $modelName", e)
        }

    }


    fun predict(input: ByteBuffer): Array<FloatArray> {
        // Предполагаем, что вывод модели имеет форму [1, 1]
        val output = Array(1) { FloatArray(1) }
        interpreter?.run(input, output)
        return output
    }

}
