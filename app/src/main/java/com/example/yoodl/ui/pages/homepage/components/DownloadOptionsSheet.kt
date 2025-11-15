package com.example.yoodl.ui.pages.homepage.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.ui.pages.homepage.HomePageVM


@Composable
fun DownloadOptionsSheet(
    viewModel: HomePageVM,
    onQueue:(DownloadQueue) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Download Audio")
        Button(
            onClick = {
                viewModel.downloadFormat(
                    viewModel.streamInfo?.webpageUrl ?: "",
                    format = null,
                    isAudio = true,
                    onQueue = { queueItem ->
                        onQueue(queueItem)
                        Toast.makeText(context, "Audio added to queue", Toast.LENGTH_SHORT).show()
                    }
                )
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Audio (MP3)")
        }

        Text("Download Video")
        viewModel.availableFormats.forEach { format ->
            val quality = format.height?.let { "${it}p" } ?: "Unknown"
            Button(
                onClick = {
                    viewModel.downloadFormat(
                        viewModel.streamInfo?.webpageUrl ?: "",
                        format = format,
                        isAudio = false,
                        onQueue = {queueItem ->
                            onQueue(queueItem)
                            Toast.makeText(context, "Video added to queue", Toast.LENGTH_SHORT).show()
                        }
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(quality)
            }
        }
    }
}


@Composable
fun DownloadOptionsSheetPL(
    viewModel: HomePageVM,
    onQueue:(DownloadQueue) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if(viewModel.isLoadingFormats(videoId = viewModel.bottomSheetPLId)){
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else {

            Text("Download Audio")
            Button(
                onClick = {
                    viewModel.downloadFormat(
                        url = viewModel.bottomSheetPLUrl,
                        videoId = viewModel.bottomSheetPLId,
                        title = viewModel.formatCache[viewModel.bottomSheetPLId]?.title ?:"Download",
                        thumbnail = viewModel.formatCache[viewModel.bottomSheetPLId]?.thumbnail ?:"",
                        format = null,
                        isAudio = true,
                        onQueue = { queueItem ->
                            onQueue(queueItem)
                            Toast.makeText(context, "Audio added to queue", Toast.LENGTH_SHORT)
                                .show()
                        }
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Audio (MP3)")
            }

            Text("Download Video")
            val availableFormats = viewModel.getFormats(videoId = viewModel.bottomSheetPLId)
            availableFormats?.forEach { format ->
                val quality = format.height.let { "${it}p" }
                Button(
                    onClick = {
                        viewModel.downloadFormat(
                            url = viewModel.bottomSheetPLUrl,
                            videoId = viewModel.bottomSheetPLId,
                            title = viewModel.formatCache[viewModel.bottomSheetPLId]?.title ?:"Download",
                            thumbnail = viewModel.formatCache[viewModel.bottomSheetPLId]?.thumbnail ?:"",
                            format = format,
                            isAudio = false,
                            onQueue = { queueItem ->
                                onQueue(queueItem)
                                Toast.makeText(context, "Video added to queue", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(quality)
                }
            }
        }
    }
}
