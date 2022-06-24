package com.example.androidndk

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import com.example.androidndk.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val convertList = GetConvertList()
    lateinit var originImage : Bitmap
    lateinit var nowImage : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nowImage = binding.imageView.drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
        originImage = nowImage.copy(Bitmap.Config.ARGB_8888, false)
        binding.listView.adapter = ArrayAdapter<String>(applicationContext,android.R.layout.simple_list_item_1, convertList)
        binding.listView.setOnItemClickListener { adapterView, view, i, l ->
            when(i){
                0 -> nowImage = originImage.copy(Bitmap.Config.ARGB_8888, true)
                1 -> bitmapToGrayScale(nowImage)
                2 -> bitmapBright(nowImage, 0.05)
                3 -> bitmapBright(nowImage, -0.05)
            }
            binding.imageView.setImageBitmap(nowImage)
        }

        binding.btnGotoCamera.setOnClickListener {
            var intent = Intent(applicationContext, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * A native method that is implemented by the 'androidndk' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
    external fun ArrayLogTest(arrayList: ArrayList<Int>)
    external fun bitmapToGrayScale(bitmap : Bitmap)
    external fun bitmapBright(bitmap: Bitmap, bright: Double)
    external fun GetConvertList(): Array<String>

    companion object {
        // Used to load the 'androidndk' library on application startup.
        init {
            System.loadLibrary("androidndk")
        }
    }
}