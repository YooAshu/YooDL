package com.example.yoodl

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.yoodl.ui.navigation.MainNavGraph
import com.example.yoodl.ui.pages.homepage.HomePageVM
import com.example.yoodl.ui.theme.YooDLTheme
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var homePageVM: HomePageVM? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        homePageVM?.let { handleIntent(intent, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YooDLTheme {
                val navController = rememberNavController()
                val vm: HomePageVM = hiltViewModel()
                this@MainActivity.homePageVM = vm

                handleIntent(intent, vm)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        homePageVM = vm
                    )
                }
            }
        }
        // Check for youtube-dl updates
        checkForYoutubeDLUpdate()
    }

    private fun checkForYoutubeDLUpdate() {
        Thread {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    this@MainActivity,
                    YoutubeDL.UpdateChannel.STABLE
                )
                Log.d("MainActivity", "YoutubeDL updated successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to update YoutubeDL: ${e.message}")
            }
        }.start()
    }
    private fun handleIntent(intent: Intent?, vm: HomePageVM) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            vm.sharedUrl.value = url
        }
    }
}
