package com.example.androidndk

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import com.example.androidndk.databinding.ActivityCameraBinding
import com.example.androidndk.databinding.ActivityMainBinding

class CameraActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var binding: ActivityCameraBinding
    var ndkCamera: Long = 0
    private lateinit var textureView: TextureView
    lateinit var surface: Surface
    private lateinit var cameraPreviewSize: Size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onWindowFocusChanged(true)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestCamera()
    }

    private fun requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
            return
        }
        createTextureView()
    }

    private fun createTextureView() {
        textureView = binding.texturePreview as TextureView
        textureView.surfaceTextureListener = this
        if (textureView.isAvailable) {
            onSurfaceTextureAvailable(textureView.surfaceTexture!!,
                textureView.width,
                textureView.height)
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        createNativeCamera()

        surfaceTexture.setDefaultBufferSize(cameraPreviewSize.width, cameraPreviewSize.height)
        surface = Surface(surfaceTexture)
        onPreviewSurfaceCreated(ndkCamera, surface)

        // textureView 크기 지정
        var newHeight = width * cameraPreviewSize.width / cameraPreviewSize.height
        textureView.layoutParams = FrameLayout.LayoutParams(width,newHeight, Gravity.CENTER)
        textureView.setTransform(Matrix().apply {
//            setScale(1f,1f)
//            postRotate(90f)
//            postTranslate(width.toFloat(), 0f)
        })
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        onPreviewSurfaceDestroyed(ndkCamera, surface)
        deleteCamera(ndkCamera,surface)
        ndkCamera = 0
        surface.release()
        return true
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

    }


    private fun createNativeCamera() {
        ndkCamera = createCamera(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)

        cameraPreviewSize = getMinimumCompatiblePreviewSize(ndkCamera)
    }

    external fun createCamera(width: Int, height: Int): Long

    external fun getMinimumCompatiblePreviewSize(ndkCamera: Long): Size

    external fun onPreviewSurfaceCreated(ndkCamera: Long, surface: Surface)

    external fun onPreviewSurfaceDestroyed(ndkCamera: Long, surface: Surface)

    external fun deleteCamera(ndkCamera: Long, surface: Surface)

}