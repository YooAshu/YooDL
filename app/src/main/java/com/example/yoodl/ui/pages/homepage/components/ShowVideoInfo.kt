package com.example.yoodl.ui.pages.homepage.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.yoodl.data.models.YtData
import com.example.yoodl.ui.pages.homepage.glassCard

@Composable
fun ShowVideoInfo(item: YtData, onDownloadClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .glassCard(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
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
                    onDownloadClick()
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

fun convertDuration(isoDuration: String): String {
    return try {
        // Parse ISO 8601 duration format (PT10M30S)
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val matchResult = regex.find(isoDuration)

        val hours = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = matchResult?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val seconds = matchResult?.groupValues?.get(3)?.toIntOrNull() ?: 0

        when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds)
            else -> String.format("0:%02d", seconds)
        }
    } catch (e: Exception) {
        "0:00"
    }
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes == 0L) return ""

    return when {
        bytes < 1024 -> "-$bytes B"
        bytes < 1024 * 1024 -> String.format("-%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("-%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("-%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
