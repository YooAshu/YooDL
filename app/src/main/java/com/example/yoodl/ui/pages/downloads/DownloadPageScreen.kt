package com.example.yoodl.ui.pages.downloads

import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.yoodl.R
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.ui.pages.homepage.components.VioletBlurBackground
import com.example.yoodl.ui.pages.homepage.glassCard
import com.example.yoodl.utils.youtube.openMediaFile
import java.io.File
import kotlin.div
import kotlin.text.toInt

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
                Column() {
                    Text("Downloads", fontWeight = FontWeight.Bold, fontSize = 24.sp)
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
                                    viewModel.showingSpeceficType = true
                                    viewModel.getDownloadedItemByType(section)
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
            if(!viewModel.showingSpeceficType) {
                item {
                    if (viewModel.queuedDownloads.isNotEmpty()) Text(
                        "Downloading",
                        fontWeight = FontWeight.Bold
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
                                    overflow = TextOverflow.Ellipsis
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
                                onClick = {}
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

                if (viewModel.queuedDownloads.size > 1) {
                    item {
                        Text(
                            "Queued (${viewModel.queuedDownloads.size - 1})",
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )
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
                                        overflow = TextOverflow.Ellipsis
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
                            fontSize = 20.sp
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
                                    overflow = TextOverflow.Ellipsis
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
                                    if (viewModel.currentDownload == null) viewModel.processDownloadQueue()
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
                        fontSize = 20.sp
                    )
                }
                items(downloadedItems) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val file = File(it.filePath)
                                if (file.exists()) {
                                    openMediaFile(context, file, it.type)
                                } else {
                                    Toast.makeText(context, "File not found", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    ) {
                        val imageBitmap = it.thumbnail?.asImageBitmap()
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
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
                                it.title,
                                maxLines = 1,
                                modifier = Modifier,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(it.type, fontSize = 12.sp, color = Color.White.copy(.5f))
                            Text(
                                viewModel.getFormattedDate(it.dateAdded),
                                fontSize = 12.sp,
                                color = Color.White.copy(.5f)
                            )
                        }
                    }
                }
            }
            else{
                item {
                    Text(
                        viewModel.nameOfShowingType,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                        fontSize = 20.sp
                    )
                }
                items(viewModel.downloadedItemByType) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val file = File(it.filePath)
                                if (file.exists()) {
                                    openMediaFile(context, file, it.type)
                                } else {
                                    Toast.makeText(context, "File not found", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    ) {
                        val imageBitmap = it.thumbnail?.asImageBitmap()
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
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
                                it.title,
                                maxLines = 1,
                                modifier = Modifier,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(it.type, fontSize = 12.sp, color = Color.White.copy(.5f))
                            Text(
                                viewModel.getFormattedDate(it.dateAdded),
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