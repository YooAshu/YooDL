package com.example.yoodl.ui.pages.homepage

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yoodl.ui.pages.downloads.DownloadPageVM
import com.example.yoodl.ui.pages.homepage.components.DownloadOptionsSheet
import com.example.yoodl.ui.pages.homepage.components.ShowVideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(
    viewModel: HomePageVM,
    downloadPageVM: DownloadPageVM,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val sheetStatePL = rememberModalBottomSheetState()
    var searchBarInput by remember { mutableStateOf("") }

    // Update when shared URL changes
    LaunchedEffect(viewModel.sharedUrl.value) {
        if (viewModel.sharedUrl.value.isNotEmpty()) {
            searchBarInput = viewModel.sharedUrl.value
            viewModel.handleYTLink(
                inputUrl = viewModel.sharedUrl.value,
                onError = { e ->
                    Toast.makeText(
                        context,
                        "Error fetching video info: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onSuccessPerVideo = {}
            )
        }
    }

    LazyColumn {
        item {
            OutlinedTextField(
                value = searchBarInput,
                onValueChange = { searchBarInput = it },
                label = { Text("Enter URL") },
                singleLine = true,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(100),
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchBarInput.isNotBlank()) {
                            viewModel.handleYTLink(
                                inputUrl = searchBarInput,
                                onError = { e ->
                                    Toast.makeText(
                                        context,
                                        "Error fetching video info: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onSuccessPerVideo = {
                                    Log.d("HomePageScreen", it)
                                }
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a valid URL",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon"
                        )
                    }
                }
            )
        }

        item {
            if (viewModel.loadingYTVideosInfo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (viewModel.ytVideosInfoEntries.isNotEmpty()) {
            item {
                if (viewModel.ytVideosInfoEntries.size > 1)
                    Text(
                        "Playlist: ${viewModel.playListName} (${viewModel.ytVideosInfoEntries.size} videos)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
            }
            items(viewModel.ytVideosInfoEntries) { item ->
                ShowVideoInfo(item = item, onDownloadClick = {
                    viewModel.showDownloadSheet = true
                    viewModel.bottomSheetCurrentVideoId = item.id
                    viewModel.bottomSheetCurrentVideoUrl = item.url
                    viewModel.fetchFormatsForVideo(item.id, item.url)
                })
            }
        }

    }

    if (viewModel.showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadSheet = false },
            sheetState = sheetStatePL
        ) {
            DownloadOptionsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showDownloadSheet = false },
                onQueue = { queueItem ->
                    downloadPageVM.addToQueue(queueItem)
                }
            )
        }

    }
}
