package com.example.yoodl.ui.pages.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun DownloadPageScreen(
    viewModel: DownloadPageVM = hiltViewModel()
) {
    LazyColumn(
        modifier = Modifier.padding(8.dp)
    ) {
        item{
            Text("Downloading", fontWeight = FontWeight.Bold)
        }



        item{
            Text("Downloaded files", fontWeight = FontWeight.Bold)
        }
        items(viewModel.downloadedItems) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                if(it.thumbnail!=null && it.thumbnail!=""){
                    AsyncImage(
                        model = it.thumbnail,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxWidth(0.2f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if(it.thumbnail==null || it.thumbnail==""){
                    Box(modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x0FC1C1C1))
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.wrapContentHeight().padding(start = 5.dp)
                ) {
                    Text(it.title, maxLines = 1, modifier = Modifier)
                    Text(it.type, fontSize = 12.sp, color = Color.White.copy(.5f))
                    Text(viewModel.getFormattedDate(it.dateAdded), fontSize = 12.sp, color = Color.White.copy(.5f))
                }

            }
        }
    }
}