package com.example.androidndk.tensorFlow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.androidndk.databinding.ActivityTensorFlowBinding

class TensorFlowActivity : AppCompatActivity() {

    lateinit var binding : ActivityTensorFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTensorFlowBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}