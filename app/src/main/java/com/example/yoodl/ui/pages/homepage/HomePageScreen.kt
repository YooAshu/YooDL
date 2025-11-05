package com.example.yoodl.ui.pages.homepage

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.yoodl.ui.pages.homepage.components.DownloadOptionsSheet
import com.example.yoodl.ui.pages.homepage.components.ShowVideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(
    viewModel: HomePageVM = hiltViewModel()
) {
    val searchBarInput = remember { mutableStateOf("") }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    LazyColumn {
        item {
            OutlinedTextField(
                value = searchBarInput.value,
                onValueChange = { searchBarInput.value = it },
                label = { Text("Enter URL") },
                singleLine = true,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(100),
                trailingIcon = {
                    IconButton(onClick = {
                        if (searchBarInput.value.isNotBlank()) {
                            viewModel.getYTVideoInfo(
                                url = searchBarInput.value,
                                onError = { e ->
                                    Toast.makeText(
                                        context,
                                        "Error fetching video info: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onSuccess = {
                                    searchBarInput.value = ""
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
    }

    if (viewModel.showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showDownloadSheet = false },
            sheetState = sheetState
        ) {
            DownloadOptionsSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.showDownloadSheet = false }
            )
        }
    }
}
