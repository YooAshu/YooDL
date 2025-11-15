package com.example.yoodl.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.yoodl.ui.pages.downloads.DownloadPageScreen
import com.example.yoodl.ui.pages.downloads.DownloadPageVM
import com.example.yoodl.ui.pages.homepage.HomePageScreen
import com.example.yoodl.ui.pages.homepage.HomePageVM

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier,homePageVM: HomePageVM) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier) {
            val downloadPageVM : DownloadPageVM = hiltViewModel()

            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomePageScreen(
                        viewModel = homePageVM,
                        downloadPageVM = downloadPageVM,
                    )
                }
                composable(Routes.DOWNLOADS) {
                    DownloadPageScreen(
                        viewModel = downloadPageVM
                    )
                }
            }

        }
        BottomNav(
            navController = navController,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

object Routes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
}