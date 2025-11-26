package com.example.yoodl.ui.pages.homepage

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.yoodl.R
import com.example.yoodl.ui.pages.downloads.DownloadPageVM
import com.example.yoodl.ui.pages.homepage.components.DownloadOptionsSheet
import com.example.yoodl.ui.pages.homepage.components.InfoDialog
import com.example.yoodl.ui.pages.homepage.components.ShowVideoInfo
import com.example.yoodl.ui.pages.homepage.components.VioletBlurBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(
    viewModel: HomePageVM,
    downloadPageVM: DownloadPageVM,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var searchBarInput by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }

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
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VioletBlurBackground()

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.yoodl),
                            contentDescription = "icon",
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "YooDL",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    }

                    IconButton(onClick = {
                        showInfoDialog = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.outline_info_24),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .glassCard() // ← GLASS
                ) {
                    OutlinedTextField(
                        value = searchBarInput,
                        onValueChange = { searchBarInput = it },
                        placeholder = { Text("Enter URL", color = Color.White.copy(0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(0.8f),
                            cursorColor = Color.White,
                            selectionColors = TextSelectionColors(
                                handleColor = Color.White,
                                backgroundColor = Color.White.copy(0.2f)
                            ),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true
                    )
                }
            }

            item {
                val violetGradient = Brush.horizontalGradient(
                    listOf(Color(0xFF8B3CFF), Color(0xFF571CFF))
                )
                Button(
                    onClick = {
                        if (searchBarInput.isNotBlank()) {
                            viewModel.handleYTLink(
                                inputUrl = searchBarInput,
                                onError = { e ->
                                    Toast.makeText(
                                        context,
                                        "Error fetching video info: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a valid URL",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(.5f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(violetGradient, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Search", color = Color.White)
                        }
                    }
                }
            }


            item {
                if (viewModel.loadingYTVideosInfo) {
//                    Log.d("HomePageScreen", "Loading... ${viewModel.loadingYTVideosInfo}")
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
            }

            if (viewModel.ytVideosInfoEntries.isNotEmpty()) {
                item {
                    if (viewModel.ytVideosInfoEntries.size > 1)
                        Text(
                            "Playlist: ${viewModel.playListName} (${viewModel.ytVideosInfoEntries.size} videos)",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp), color = Color.White
                        )
                }
                items(viewModel.ytVideosInfoEntries) { item ->
                    ShowVideoInfo(item = item, onDownloadClick = {
                        viewModel.showDownloadSheet = true
                        viewModel.bottomSheetCurrentVideo = item
                        viewModel.fetchFormatsForVideo(item.id, item.url)
                    })
                }
            }

        }
    }

    if (viewModel.showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF070715)
        ) {
            DownloadOptionsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showDownloadSheet = false },
                onQueue = { queueItem ->
                    downloadPageVM.addToQueue(queueItem)
//                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                },
                item = viewModel.bottomSheetCurrentVideo
            )
        }

    }
    if (showInfoDialog) {
        InfoDialog(onDismiss = { showInfoDialog = false })
    }
}

fun Modifier.glassCard(corner: Int = 30): Modifier = this
    .clip(RoundedCornerShape(corner.dp))
    .background(Color.White.copy(alpha = 0.08f))
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(corner.dp)
    )
//    .blur(30.dp)
