package com.example.yoodl.ui.pages.downloads

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.yoodl.R
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.ui.pages.homepage.components.VioletBlurBackground
import com.example.yoodl.ui.pages.homepage.glassCard
import com.example.yoodl.utils.youtube.openMediaFile
import java.io.File

@Composable
fun DownloadPageScreen(
    viewModel: DownloadPageVM = hiltViewModel()
) {
    val context = LocalContext.current
    val downloadedItems by viewModel.downloadedItems.collectAsState()

    Box(
        Modifier.fillMaxSize()
    ) {
        VioletBlurBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            item {
                Column {
                    Text(
                        "Downloads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "all",
                            "audio",
                            "video",
                            "youtube",
                            "instagram",
                            "facebook"
                        ).forEach { section ->
                            Button(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(horizontal = 5.dp)
                                    .glassCard(10),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(4.dp),
                                onClick = {
//                                    viewModel.logAllDownloads()
                                    if (viewModel.specificType != section) {
                                        viewModel.specificType = section
                                        viewModel.showingSpecificType = true
                                        viewModel.getDownloadedItemByType(section)
                                    }
                                },
                            ) {
                                Text(
                                    section,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                    }
                }
            }
            if (!viewModel.showingSpecificType) {
                item {
                    if (viewModel.currentDownload != null) Text(
                        "Downloading",
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
                item {
                    viewModel.currentDownload?.let { current ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            if (current.thumbnail != null && current.thumbnail!!.isNotEmpty()) {
                                AsyncImage(
                                    model = current.thumbnail,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x0FC1C1C1))
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(.7f)
                                    .wrapContentHeight()
                                    .padding(start = 5.dp)
                            ) {
                                Text(
                                    current.title,
                                    maxLines = 1,
                                    modifier = Modifier,
                                    overflow = TextOverflow.Ellipsis, color = Color.White
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        if (current.isAudio) "audio" else "video",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(.5f)
                                    )
                                    Text(
                                        if (viewModel.downloadProgress < 0) "Connecting.." else if (viewModel.downloadProgress >= 100) "Processing" else "Downloading",
                                        color = Color.White.copy(.5f),
                                        fontSize = 10.sp,
                                    )
                                    Text(
                                        "${viewModel.downloadProgress}%",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(.7f)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { viewModel.downloadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF6830FF),
                                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                                )

                            }
                            IconButton(
                                modifier = Modifier.weight(.1f),
                                onClick = {
                                    if (current.status == DownloadStatus.DOWNLOADING) {
                                        if (viewModel.downloadProgress !in 0.0..<100.0) {
                                            Toast.makeText(
                                                context,
                                                "Cannot pause while Connecting or Processing",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else
                                            viewModel.pauseDownload(current.id)
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id =
                                            when (current.status) {
                                                DownloadStatus.DOWNLOADING -> R.drawable.pause
                                                DownloadStatus.PAUSED -> R.drawable.play
                                                else -> R.drawable.close_svgrepo_com
                                            }
                                    ),
                                    tint = Color.White,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                if (viewModel.queuedDownloads.isNotEmpty()) {
                    item {
                        if (!(viewModel.currentDownload != null && viewModel.queuedDownloads.size == 1)) {
                            val size =
                                viewModel.currentDownload.let { if (it != null) viewModel.queuedDownloads.size - 1 else viewModel.queuedDownloads.size }

                            Text(
                                "Queued ($size)",
                                fontWeight = FontWeight.Bold,
                                lineHeight = 36.sp, color = Color.White
                            )
                        }
                    }
                    items(viewModel.queuedDownloads) { queued ->
                        if (queued.id != viewModel.currentDownload?.id) {

                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                if (queued.thumbnail != null && queued.thumbnail!!.isNotEmpty()) {
                                    AsyncImage(
                                        model = queued.thumbnail,
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier
                                            .fillMaxWidth(0.2f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.2f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x0FC1C1C1))
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.Top,
                                    modifier = Modifier
                                        .weight(.7f)
                                        .wrapContentHeight()
                                        .padding(start = 5.dp)
                                ) {
                                    Text(
                                        queued.title,
                                        maxLines = 1,
                                        modifier = Modifier,
                                        overflow = TextOverflow.Ellipsis, color = Color.White
                                    )
                                    Text(
                                        if (queued.isAudio) "audio" else "video",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(.5f)
                                    )

                                }
                                IconButton(
                                    modifier = Modifier.weight(.1f),
                                    onClick = {
                                        if (queued.status == DownloadStatus.PAUSED) {
                                            //update that queue with status pending
                                            viewModel.updateQueuedDownloadStatus(
                                                queued.id,
                                                DownloadStatus.PENDING
                                            )
                                            viewModel.processDownloadQueue()
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id =
                                                when (queued.status) {
                                                    DownloadStatus.DOWNLOADING -> R.drawable.pause
                                                    DownloadStatus.PAUSED -> R.drawable.play
                                                    DownloadStatus.PENDING -> R.drawable.waiting
                                                    else -> R.drawable.close_svgrepo_com
                                                }
                                        ),
                                        tint = Color.White,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                            }
                        }
                    }
                }

                if (viewModel.failedDownloads.isNotEmpty()) {
                    item {
                        Text(
                            "Failed",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            fontSize = 20.sp, color = Color.White
                        )
                    }
                    items(viewModel.failedDownloads) { failed ->
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (failed.thumbnail != null && failed.thumbnail!!.isNotEmpty()) {
                                AsyncImage(
                                    model = failed.thumbnail,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x0FC1C1C1))
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier
                                    .weight(.7f)
                                    .wrapContentHeight()
                                    .padding(start = 5.dp)
                            ) {
                                Text(
                                    failed.title,
                                    maxLines = 1,
                                    modifier = Modifier,
                                    overflow = TextOverflow.Ellipsis, color = Color.White
                                )
                                Text(
                                    if (failed.isAudio) "audio" else "video",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(.5f)
                                )

                            }
                            IconButton(
                                modifier = Modifier.weight(.1f),
                                onClick = {
                                    viewModel.moveFromFailedToQueued(failed.id)
//                                    if (viewModel.currentDownload == null) viewModel.processDownloadQueue()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    tint = Color.White,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Downloaded",
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                        fontSize = 20.sp, color = Color.White
                    )
                }
                items(downloadedItems) { downloadItem ->

                    var showDialog by remember { mutableStateOf(false) }

                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            title = {
                                Text(
                                    downloadItem.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            text = {
                                Text("Delete this download?", color = Color.White)
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.removeDownload(downloadItem.id, onDelete = {
                                        Toast.makeText(
                                            context,
                                            "Deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                                    showDialog = false
                                }) {
                                    Text("Delete", color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                            },
                            containerColor = Color(0xFF070715)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { showDialog = true },
                                    onTap = {
                                        val file = File(downloadItem.filePath)
                                        if (file.exists()) {
                                            openMediaFile(context, file, downloadItem.type)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "File not found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                    ) {
                        val imageBitmap = downloadItem.thumbnail?.asImageBitmap()
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier
                                    .fillMaxWidth(0.2f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.2f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x0FC1C1C1))
                            )
                        }
                        Column(
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(start = 5.dp)
                        ) {
                            Text(
                                downloadItem.title,
                                maxLines = 1,
                                modifier = Modifier,
                                overflow = TextOverflow.Ellipsis, color = Color.White
                            )
                            Text(downloadItem.type, fontSize = 12.sp, color = Color.White.copy(.5f))
                            Text(
                                viewModel.getFormattedDate(downloadItem.dateAdded),
                                fontSize = 12.sp,
                                color = Color.White.copy(.5f)
                            )
                        }
                    }
                }
            } else {
                if (viewModel.loadingSpecificType) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            viewModel.nameOfShowingType,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            fontSize = 20.sp, color = Color.White
                        )
                    }
                    items(viewModel.downloadedItemByType) { item->
                        var showDialog by remember { mutableStateOf(false) }

                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = {
                                    Text(
                                        item.title,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                text = {
                                    Text("Delete this download?", color = Color.White)
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.removeTypeDownload(item.id, onDelete = {
                                            Toast.makeText(
                                                context,
                                                "Deleted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        })
                                        showDialog = false
                                    }) {
                                        Text("Delete", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                },
                                containerColor = Color(0xFF070715)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { showDialog = true },
                                        onTap = {
                                            val file = File(item.filePath)
                                            if (file.exists()) {
                                                openMediaFile(context, file, item.type)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "File not found",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }

                        ) {
                            val imageBitmap = item.thumbnail?.asImageBitmap()
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.2f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x0FC1C1C1))
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.Top,
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .padding(start = 5.dp)
                            ) {
                                Text(
                                    item.title,
                                    maxLines = 1,
                                    modifier = Modifier,
                                    overflow = TextOverflow.Ellipsis, color = Color.White
                                )
                                Text(item.type, fontSize = 12.sp, color = Color.White.copy(.5f))
                                Text(
                                    viewModel.getFormattedDate(item.dateAdded),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

