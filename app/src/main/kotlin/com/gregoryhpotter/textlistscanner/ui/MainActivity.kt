package com.gregoryhpotter.textlistscanner.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.gregoryhpotter.textlistscanner.R
import com.gregoryhpotter.textlistscanner.data.repository.SettingsRepository
import com.gregoryhpotter.textlistscanner.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        if (!settingsRepository.hasSeenOnboarding) {
            settingsRepository.hasSeenOnboarding = true
            binding.bottomNav.selectedItemId = R.id.wordListFragment
        }
    }
}
