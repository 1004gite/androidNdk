package com.example.androidndk.tensorFlow

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.androidndk.databinding.FragmentCameraBinding
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.time.LocalTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var binding: FragmentCameraBinding? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var bitmapBuffer: Bitmap
    private var camera: Camera? = null
    private lateinit var objectDetect: ObjectDetect
    private var overlayBitmap: Bitmap? = null
    private var clearBitmap: Bitmap? = null
    private var paint = Paint().apply {
        textSize = 50f
        color = Color.BLUE
    }
    var useKotlin = false
    var time = SystemClock.uptimeMillis()
    var wP = 300f
    var hP = 300f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentCameraBinding.inflate(inflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        wP = resources.displayMetrics.widthPixels.toFloat()
        hP = resources.displayMetrics.heightPixels.toFloat()
        clearBitmap = Bitmap.createBitmap(wP.toInt(),
            hP.toInt(),
            Bitmap.Config.ARGB_8888).also {
            for (row in 0 until it!!.height) {
                for (col in 0 until it!!.width) {
                    it!!.setPixel(col, row, Color.argb(0, 0, 0, 0))
                }
            }
        }
        setSwitch()
        cameraExecutor = Executors.newSingleThreadExecutor()
        objectDetect = ObjectDetect(resources.assets)
        // view가 준비되면 카메라 세팅
        binding!!.previewView?.post {
            setupCamera()
        }
    }

    private fun setSwitch() {
        binding!!.useKotSwitch.setOnClickListener {
            useKotlin = (it as Switch).isChecked
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cameraUseCases()
        },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun cameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
//            CameraSelector.DEFAULT_BACK_CAMERA

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding!!.previewView.display.rotation)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding!!.previewView.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 생산속도 조절 관련
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { img ->
                    if (!::bitmapBuffer.isInitialized) {
                        bitmapBuffer =
                            Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
                    }

                    time = SystemClock.uptimeMillis()
                    // 여기서 객체 감지
                    img.use { bitmapBuffer.copyPixelsFromBuffer(img.planes[0].buffer) }
                    var byteBuffer: ByteBuffer? = null
                    if (useKotlin) {
                        byteBuffer = ByteBuffer.wrap(getNormailzedByteArrWithKotlin())
                    } else {
                        byteBuffer = ByteBuffer.wrap(
                            bitmapToByteArray(bitmapBuffer, img.width, img.height, 300, 300))
                    }

                    var result = objectDetect.predict(byteBuffer!!)
                    time = SystemClock.uptimeMillis()-time
                    drawBox(result)
                }
            }

        // 리바인딩 하기전에 모두 바인딩 해제
        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            preview?.setSurfaceProvider(binding!!.previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraProviderErr", "bind error")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun getNormailzedByteArrWithKotlin(): ByteArray {
        var tmp = Bitmap.createScaledBitmap(bitmapBuffer, 300, 300, false)
        var byteBuffer: ByteBuffer = ByteBuffer.allocate(tmp.byteCount)
        tmp.copyPixelsToBuffer(byteBuffer)
        var byteArray = byteBuffer.array().asUByteArray()
        var result = ByteArray(300 * 300 * 3)
        var R: UByte = 0u
        var G: UByte = 0u
        var B: UByte = 0u
        var r: UByte = 255u
        var g: UByte = 255u
        var b: UByte = 255u
        for (row in 0 until 300) {
            val indexR = row * 300 * 4
            for (col in 0 until 300) {
                val index = indexR + col * 4
                R = if (R > byteArray[index + 0]) R else byteArray[index + 0]
                G = if (G > byteArray[index + 1]) G else byteArray[index + 1]
                B = if (B > byteArray[index + 2]) B else byteArray[index + 2]
                r = if (r < byteArray[index + 0]) r else byteArray[index + 0]
                g = if (g < byteArray[index + 1]) g else byteArray[index + 1]
                b = if (b < byteArray[index + 2]) b else byteArray[index + 2]
            }
        }
        var ratioR = (255.0 / ((R - r + 1u).toDouble())).toInt().toUByte()
        var ratioG = (255.0 / ((G - g + 1u).toDouble())).toInt().toUByte()
        var ratioB = (255.0 / ((B - b + 1u).toDouble())).toInt().toUByte()
        for (row in 0 until 300) {
            val indexR = row * 300 * 4
            val indexR2 = row * 300 * 3
            for (col in 0 until 300) {
                val index = indexR + col * 4
                val index2 = indexR2 + col * 3
                result[index2 + 0] = ((byteArray[index + 0] - r) * ratioR).toByte()
                result[index2 + 1] = ((byteArray[index + 1] - g) * ratioG).toByte()
                result[index2 + 2] = ((byteArray[index + 2] - b) * ratioB).toByte()
            }
        }

        return result
    }

    fun drawBox(result: MutableMap<Int, Any>) {
        var point = (result[0] as Array<*>)[0] as Array<FloatArray>
        var classifier = (result[1] as Array<*>)[0] as FloatArray
        var confidence = (result[2] as Array<*>)[0] as FloatArray
        overlayBitmap = Bitmap.createBitmap(clearBitmap!!)

        for (index in point.indices) {
            var arr = point[index]
            if (confidence[index] <= 0.5f) continue //정확도

            var canvas = Canvas().also {
                it.setBitmap(overlayBitmap!!)
            }
            var startX = (arr[1] * wP).let {
                if (it < 0f) 0f
                else if (it >= wP) wP
                else it
            }
            var startY = (arr[0] * hP).let {
                if (it < 0f) 0f
                else if (it >= hP) hP
                else it
            }
            var endX = (arr[3] * wP).let {
                if (it < 0f) 0f
                else if (it >= wP) wP
                else it
            }
            var endY = (arr[2] * hP).let {
                if (it < 0f) 0f
                else if (it >= hP) hP
                else it
            }
            canvas.drawLine(startX, startY, endX, startY, paint)
            canvas.drawLine(startX, endY, endX, endY, paint)
            canvas.drawLine(startX, startY, startX, endY, paint)
            canvas.drawLine(endX, startY, endX, endY, paint)
            canvas.drawText(label[classifier[index].toInt()], startX + 5f, startY + 50f, paint)

        }
        activity?.runOnUiThread {
            binding?.overlayImage?.setImageBitmap(overlayBitmap)
            binding?.timeText?.text =
                "time: " + time.toString() + "ms"
        }

        Thread.sleep(500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        cameraProvider?.unbindAll()
        imageAnalysis = null
        camera = null
        preview = null
    }

    external fun bitmapToByteArray(
        bitmap: Bitmap,
        w: Int,
        h: Int,
        targetW: Int,
        targetH: Int,
    ): ByteArray

    val label = arrayOf<String>(
        "person",
        "bicycle",
        "car",
        "motorcycle",
        "airplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "???",
        "stop sign",
        "parking meter",
        "bench",
        "bird",
        "cat",
        "dog",
        "horse",
        "sheep",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "???",
        "backpack",
        "umbrella",
        "???",
        "???",
        "handbag",
        "tie",
        "suitcase",
        "frisbee",
        "skis",
        "snowboard",
        "sports ball",
        "kite",
        "baseball bat",
        "baseball glove",
        "skateboard",
        "surfboard",
        "tennis racket",
        "bottle",
        "???",
        "wine glass",
        "cup",
        "fork",
        "knife",
        "spoon",
        "bowl",
        "banana",
        "apple",
        "sandwich",
        "orange",
        "broccoli",
        "carrot",
        "hot dog",
        "pizza",
        "donut",
        "cake",
        "chair",
        "couch",
        "potted plant",
        "bed",
        "???",
        "dining table",
        "???",
        "???",
        "toilet",
        "???",
        "tv",
        "laptop",
        "mouse",
        "remote",
        "keyboard",
        "cell phone",
        "microwave",
        "oven",
        "toaster",
        "sink",
        "refrigerator",
        "???",
        "book",
        "clock",
        "vase",
        "scissors",
        "teddy bear",
        "hair drier",
        "toothbrush"
    )
}