package com.example.androidndk.tensorFlow

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * 모델은 이미지를 입력으로 사용합니다.
 * 예상 이미지가 300x300픽셀이고 픽셀당 3개의 채널(빨간색, 파란색 및 녹색)이 있다고 가정합니다.
 * 이것은 270,000바이트 값(300x300x3)의 병합된 버퍼로 모델에 제공되어야 합니다.
 * 모델이 양자화 되면 각 값은 0에서 255 사이의 값을 나타내는 단일 바이트여야 합니다.
 * */
class ObjectDetect(private val assetManager: AssetManager) {
    val modelName = "ssd_mobilenet_v1_1_metadata_1.tflite"
    lateinit var interpreter: Interpreter

    init {
        val model = loadModelFile()
        model.order(ByteOrder.nativeOrder())
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = assetManager.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(byteBuffer: ByteBuffer): MutableMap<Int, Any> {
        var result = mutableMapOf<Int, Any>()
            .also {
                it.put(0, Array(1) { Array(10) { FloatArray(4) } })
                it.put(1, Array(1) { FloatArray(10) })
                it.put(2, Array(1) { FloatArray(10) })
                it.put(3, FloatArray(1))
            }
        interpreter.runForMultipleInputsOutputs(arrayOf(byteBuffer), result)
//        interpreter.run(byteBuffer, result)
        return result
    }
}