package com.gregoryhpotter.textlistscanner.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gregoryhpotter.textlistscanner.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Toolbar is set by CameraFragment via setSupportActionBar()
    }
}