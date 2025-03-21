package com.example.qrshieldapp

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteModel(context: Context) {
    private var interpreter: Interpreter? = null // Use nullable to avoid crashes

    init {
        try {
            interpreter = Interpreter(loadModelFile(context))
        } catch (e: Exception) {
            Log.e("TFLiteModel", "Error loading model: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        return try {
            val fileDescriptor = context.assets.openFd("model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("TFLiteModel", "Error loading model file: ${e.message}")
            throw RuntimeException("Failed to load model.tflite") // Ensure app doesn't continue with an invalid model
        }
    }

    fun predict(inputData: FloatArray): Float {
        return try {
            val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4)
                .order(ByteOrder.nativeOrder())
            for (value in inputData) {
                inputBuffer.putFloat(value)
            }

            val outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

            interpreter?.run(inputBuffer, outputBuffer) ?: throw IllegalStateException("Interpreter is not initialized!")

            outputBuffer.rewind()
            outputBuffer.float
        } catch (e: Exception) {
            Log.e("TFLiteModel", "Error running prediction: ${e.message}")
            -1f // Return a default error value
        }
    }
}
