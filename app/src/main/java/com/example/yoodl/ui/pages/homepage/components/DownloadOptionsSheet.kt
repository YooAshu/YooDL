package com.example.yoodl.ui.pages.homepage.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.yoodl.ui.pages.homepage.HomePageVM


@Composable
fun DownloadOptionsSheet(
    viewModel: HomePageVM,
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
                    null,
                    isAudio = true,
                    onSuccess = {
                        Toast.makeText(context, "Audio downloaded successfully", Toast.LENGTH_SHORT).show()
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
                        format,
                        isAudio = false,
                        onSuccess = {
                            Toast.makeText(context, "Video downloaded successfully", Toast.LENGTH_SHORT).show()
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
