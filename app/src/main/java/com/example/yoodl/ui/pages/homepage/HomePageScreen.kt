package com.example.yoodl.ui.pages.homepage

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.yoodl.ui.pages.downloads.DownloadPageVM
import com.example.yoodl.ui.pages.homepage.components.DownloadOptionsSheet
import com.example.yoodl.ui.pages.homepage.components.DownloadOptionsSheetPL
import com.example.yoodl.ui.pages.homepage.components.ShowVideoInfo
import com.example.yoodl.ui.pages.homepage.components.convertDuration
import com.example.yoodl.ui.pages.homepage.components.formatDuration

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
            if(viewModel.isPlaylistUrl(viewModel.sharedUrl.value)) {
                viewModel.handleSharedOrTypedUrl(
                    inputUrl = viewModel.sharedUrl.value,
                    onError = {},
                    onSuccessPerVideo = {}
                )
            }
            else {
                viewModel.getYTVideoInfo(
                    url = viewModel.sharedUrl.value,
                    onError = { e ->
                        Toast.makeText(
                            context,
                            "Error fetching video info: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onSuccess = {
                        // Keep the URL in searchBarInput so user sees what was processed
                    }
                )
            }
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
                            if (viewModel.isPlaylistUrl(searchBarInput)) {
                                viewModel.handleSharedOrTypedUrl(
                                    inputUrl = searchBarInput,
                                    onError = {},
                                    onSuccessPerVideo = {
                                        Log.d("HomePageScreen", it)
                                    }
                                )
                            } else {
                                viewModel.getYTVideoInfo(
                                    url = searchBarInput,
                                    onError = { e ->
                                        Toast.makeText(
                                            context,
                                            "Error fetching video info: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onSuccess = {
//                                        searchBarInput = ""
                                    }
                                )
                            }

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
            if (viewModel.loadingInfo) {
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

        item {
            viewModel.streamInfo?.let {
                ShowVideoInfo(info = it) {
                    viewModel.showDownloadSheet = true
                }
            }
        }
        item {
            if (viewModel.loadingPlaylist) {
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

        if (viewModel.playlistEntries.isNotEmpty()) {
            item {
                Text(
                    "Playlist: ${viewModel.playListName} (${viewModel.playlistEntries.size} videos)",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(viewModel.playlistEntries) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Thumbnail
                        AsyncImage(
                            model = item.thumbnail,
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Uploader
                        Text(
                            text = "By: ${item.channelName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

//                         Duration

                        Text(
                            text = "Duration: ${convertDuration(item.duration)}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Download button
                        Button(
                            onClick = {
                                viewModel.showDownloadSheetPL = true
                                viewModel.bottomSheetPLId = item.id
                                viewModel.bottomSheetPLUrl = item.url
                                viewModel.fetchFormatsForVideo(item.id, item.url)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }

    }

    if (viewModel.showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadSheet = false },
            sheetState = sheetState
        ) {
            DownloadOptionsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showDownloadSheet = false },
                onQueue = { queueItem ->
                    queueItem.thumbnail = viewModel.streamInfo?.thumbnail
                    downloadPageVM.addToQueue(queueItem)
                }
            )
        }

    }

    if (viewModel.showDownloadSheetPL) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadSheetPL = false },
            sheetState = sheetStatePL
        ) {
            DownloadOptionsSheetPL(
                viewModel = viewModel,
                onDismiss = { viewModel.showDownloadSheetPL = false },
                onQueue = { queueItem ->
                    downloadPageVM.addToQueue(queueItem)
                }
            )
        }

    }
}
