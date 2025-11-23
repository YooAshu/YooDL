package com.example.yoodl.ui.pages.homepage.components

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.YtData
import com.example.yoodl.ui.pages.homepage.HomePageVM
import com.example.yoodl.ui.pages.homepage.glassCard


@Composable
fun DownloadOptionsSheet(
    viewModel: HomePageVM,
    onQueue: (DownloadQueue) -> Unit,
    onDismiss: () -> Unit,
    item: YtData?
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .glassCard()
        ) {
            AsyncImage(
                model = item?.thumbnail ?: "",
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(
                        RoundedCornerShape(25.dp)
                    ),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 5.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    item?.title ?: "Download",
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "by : ${item?.channelName ?: "Channel"}",
                    maxLines = 1,
                    fontSize = 12.sp,
                    color = Color.White.copy(.5f)
                )
                Text(
                    convertDuration(item?.duration ?: ""),
                    maxLines = 1,
                    fontSize = 12.sp,
                    color = Color.White.copy(.5f)
                )
            }
        }

        if (viewModel.isLoadingFormats(videoId = item!!.id)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            Text("Download Audio", fontWeight = FontWeight.Bold)
            val availableAudioFormats =
                viewModel.getAudioFormats(videoId = item.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                availableAudioFormats?.forEach { format ->
                    if (format.abr == 0) return@forEach
                    Button(
                        onClick = {
                            viewModel.downloadFormat(
                                url = item.url,
                                videoId = item.id,
                                title = item.title,
                                thumbnail = item.thumbnail,
                                format = format,
                                isAudio = true,
                                onQueue = { queueItem ->
                                    onQueue(queueItem)
                                    Toast.makeText(
                                        context,
                                        "Audio added to queue",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .wrapContentWidth()
                            .glassCard()
                    ) {
                        val audioBitrate = format.abr
                        val size = formatFileSize(format.fileSizeApproximate)
                        Text("${audioBitrate}kbps $size")
                    }
                    Spacer(Modifier.width(10.dp))
                }
            }

            Text("Download Video", fontWeight = FontWeight.Bold)
            val availableFormats =
                viewModel.getVideoFormats(videoId = item.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                availableFormats?.forEach { format ->
                    val quality = format.height.let { "${it}p" }
                    Button(
                        onClick = {
                            viewModel.downloadFormat(
                                url = item.url,
                                videoId = item.id,
                                title = item.title,
                                thumbnail = item.thumbnail,
                                format = format,
                                isAudio = false,
                                onQueue = { queueItem ->
                                    onQueue(queueItem)
                                    Toast.makeText(
                                        context,
                                        "Video added to queue",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .wrapContentWidth()
                            .glassCard()
                    ) {
                        val size = formatFileSize(format.fileSizeApproximate)
                        Text(quality + size)
                    }
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
    }
}
