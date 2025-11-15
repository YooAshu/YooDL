// kotlin
package com.example.yoodl.ui.pages.homepage

import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yoodl.data.api.RetrofitInstance
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.data.models.YtData
import com.example.yoodl.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.text.isNotEmpty

@HiltViewModel
class HomePageVM @Inject constructor(
    val repo: DownloadRepository
) : ViewModel() {
    private val baseDownloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YooDL/youtube"
    )
    val sharedUrl = mutableStateOf("")
    var showDownloadSheet by mutableStateOf(false)
    var bottomSheetCurrentVideoId by mutableStateOf("")
    var bottomSheetCurrentVideoUrl by mutableStateOf("")
    var lastFetchedPlaylistUrl by mutableStateOf("")

    var ytVideosInfoEntries by mutableStateOf<List<YtData>>(emptyList())
    var loadingYTVideosInfo by mutableStateOf(false)
    var playListName by mutableStateOf("")

    // Cache of fetched formats (videoId -> VideoInfo)
    val formatCache = mutableStateMapOf<String, VideoInfo?>()

    // Track which videos are currently loading formats
    val loadingFormats = mutableStateMapOf<String, Boolean>()

    // Active jobs for each video (to cancel when needed)
    private val fetchJobs = mutableMapOf<String, Job>()


    fun downloadFormat(
        url: String,
        videoId: String = "",
        title: String = "Download",
        thumbnail: String = "",
        format: VideoFormat?,
        isAudio: Boolean,
        onQueue: (DownloadQueue) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val baseDownloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "YooDL/youtube"
                )

                val downloadDir = if (isAudio) {
                    File(baseDownloadDir, "audio")
                } else {
                    File(baseDownloadDir, "video")
                }
                downloadDir.mkdirs()

                val quality =
                    if (isAudio) "audio" else format?.height?.let { "${it}p" } ?: "unknown"

                val queueItem = DownloadQueue(
                    id = videoId,
                    title = title,
                    url = url,
                    format = format,
                    isAudio = isAudio,
                    status = DownloadStatus.PENDING,
                    progress = 0,
                    eta = 0,
                    filePath = "${downloadDir.absolutePath}/$title [$quality]",
                    thumbnail = thumbnail
                )

                onQueue(queueItem)

            } catch (e: Exception) {
                Log.e("Download", "Error: ${e.message}")
            }
        }
    }

    fun isPlaylistUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("list=") ||
                lower.contains("youtube.com/playlist") ||
                lower.contains("youtube.com/watch") && lower.contains("list=") ||
                lower.contains("youtu.be") && lower.contains("list=")
    }


    fun handleYTLink(
        inputUrl: String,
        onError: (Exception) -> Unit = {},
        onSuccessPerVideo: (String) -> Unit = {}
    ) {
        if (inputUrl.isBlank()) return
        // Skip if same URL was already successfully fetched
        if (inputUrl == lastFetchedPlaylistUrl && ytVideosInfoEntries.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            //change
            loadingYTVideosInfo = true
            ytVideosInfoEntries = emptyList()
            formatCache.clear()
            loadingFormats.clear()
            fetchJobs.clear()
            if (isPlaylistUrl(inputUrl)) {
                val playlistId = extractPlaylistId(inputUrl)
                if (playlistId.isBlank()) {
                    onError(Exception("Invalid playlist URL"))
                    loadingYTVideosInfo = false
                    return@launch
                }

                val entries = try {
                    fetchPlaylistEntriesViaAPI(playlistId)
                } catch (e: Exception) {
                    Log.e("HandleURL", "Error: ${e.message}", e)
                    onError(e)
                    emptyList()
                }

                ytVideosInfoEntries = entries
                lastFetchedPlaylistUrl = inputUrl
                loadingYTVideosInfo = false

                if (entries.isEmpty()) {
                    onError(Exception("No playlist entries found"))
                }
            } else {
                if (isValidYouTubeVideoUrl(inputUrl)) {
                    val videoId = extractVideoIdFromUrl(inputUrl)
                    if (videoId == null) {
                        onError(Exception("Invalid video URL"))
                        loadingYTVideosInfo = true
                        return@launch
                    }
                    val entry = try {
                        fetchSingleVideoViaAPI(videoId)
                    } catch (
                        e: Exception
                    ) {
                        Log.e("HandleURL", "Error: ${e.message}", e)
                        onError(e)
                        null
                    }
                    ytVideosInfoEntries = listOfNotNull(entry)
                    lastFetchedPlaylistUrl = inputUrl
                    loadingYTVideosInfo = false
                }
                else{
                    //handle invalid yt url
                }
            }
        }
    }

    private suspend fun fetchPlaylistEntriesViaAPI(playlistId: String): List<YtData> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "AIzaSyDKmZ1uGGBCZcv04dQ9F-aiLKvfQEfAyOw"
                val service = RetrofitInstance.getYouTubeApiService()

                // Fetch playlist name
                val playlistResponse = service.getPlaylistInfo("snippet", playlistId, apiKey)
                playListName = playlistResponse.items.firstOrNull()?.snippet?.title ?: "Unknown"

                // Fetch all video IDs - title - thumbnail - title
                val results =  mutableMapOf<String, YtData?>()
                val videoIds = mutableListOf<String>()
                var pageToken = ""
                do {
                    val itemsResponse = service.getPlaylistItems(
                        "contentDetails,snippet",
                        playlistId,
                        50,
                        pageToken,
                        apiKey
                    )
                    itemsResponse.items.forEach { item ->
                        val videoId = item.contentDetails?.videoId ?: return@forEach
                        results[videoId]=
                            YtData(
                                id = videoId,
                                title = item.snippet?.title ?: "Unknown",
                                url = "https://www.youtube.com/watch?v=$videoId",
                                thumbnail = item.snippet?.thumbnails?.maxres?.url
                                    ?: item.snippet?.thumbnails?.high?.url ?: "",
                                duration = "",
                                channelName = item.snippet?.channelTitle ?: "Unknown"
                            )

                    }
                    videoIds.addAll(itemsResponse.items.mapNotNull { it.contentDetails?.videoId })
                    pageToken = itemsResponse.nextPageToken ?: ""
                } while (pageToken.isNotEmpty())

                // Fetch durations in batches
                for (i in videoIds.indices step 50) {
                    val ids = videoIds.subList(i, minOf(i + 50, videoIds.size)).joinToString(",")
                    val videosResponse = service.getVideoDurations("contentDetails", ids, apiKey)
                    videosResponse.items.forEach { video ->
                        results[video.id]?.duration = video.contentDetails?.duration ?: ""
                    }
                }

                Log.d("PlaylistAPI", "Fetched ${results.size} videos from API")
                results.values.filterNotNull().toList()
            } catch (e: Exception) {
                Log.e("PlaylistAPI", "Error: ${e.message}", e)
                emptyList()
            }
        }
    }

    private suspend fun fetchSingleVideoViaAPI(videoId: String): YtData? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = "AIzaSyDKmZ1uGGBCZcv04dQ9F-aiLKvfQEfAyOw"
                val service = RetrofitInstance.getYouTubeApiService()

                val videosResponse = service.getSingleVideoInfo(
                    "snippet,contentDetails",
                    videoId,
                    apiKey
                )

                val videoItem = videosResponse.items.firstOrNull() ?: return@withContext null

                YtData(
                    id = videoItem.id,
                    title = videoItem.snippet?.title ?: "Unknown",
                    url = "https://www.youtube.com/watch?v=${videoItem.id}",
                    thumbnail = videoItem.snippet?.thumbnails?.maxres?.url
                        ?: videoItem.snippet?.thumbnails?.high?.url ?: "",
                    duration = videoItem.contentDetails?.duration ?: "",
                    channelName = videoItem.snippet?.channelTitle ?: "Unknown"
                )
            } catch (e: Exception) {
                Log.e("SingleVideoAPI", "Error: ${e.message}", e)
                null
            }
        }
    }



    fun extractVideoIdFromUrl(url: String): String? {
        return try {
            when {
                // Standard youtube.com format: https://www.youtube.com/watch?v=VIDEO_ID
                url.contains("youtube.com/watch") -> {
                    url.split("v=")[1].split("&")[0]
                }
                // Short youtu.be format: https://youtu.be/VIDEO_ID
                url.contains("youtu.be/") -> {
                    url.split("youtu.be/")[1].split("?")[0].split("&")[0]
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun isValidYouTubeVideoUrl(url: String): Boolean {
        val videoId = extractVideoIdFromUrl(url)
        return videoId != null && videoId.length == 11 && videoId.matches(Regex("[a-zA-Z0-9_-]+"))
    }


    private fun extractPlaylistId(url: String): String {
        return try {
            val listParam = url.split("list=")[1].split("&")[0]
            listParam
        } catch (e: Exception) {
            ""
        }
    }

    fun fetchFormatsForVideo(videoId: String, videoUrl: String) {
        // If already cached, return immediately
        if (formatCache.containsKey(videoId)) {
            Log.d("Formats", "Using cached formats for $videoId")
            return
        }

        // If already loading, don't start again
        if (loadingFormats[videoId] == true) {
            Log.d("Formats", "Already loading formats for $videoId")
            return
        }

        // Start fetching
        loadingFormats[videoId] = true

        fetchJobs[videoId] = viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoInfo = YoutubeDL.getInstance().getInfo(videoUrl)

                withContext(Dispatchers.Main) {
                    formatCache[videoId] = videoInfo
                    loadingFormats[videoId] = false
                    Log.d("Formats", "✅ Formats loaded for $videoId")
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        loadingFormats[videoId] = false
                        Log.e("Formats", "❌ Error loading formats for $videoId: ${e.message}")
                    }
                }
            }
        }
    }

    // Get formats for a video (from cache)
    fun getFormats(videoId: String): List<VideoFormat>? {
        return formatCache[videoId]?.formats
            ?.filter { format ->
                format.vcodec != null &&
                        format.vcodec != "none" &&
                        format.height != null &&
                        format.height > 0
            }
            ?.sortedByDescending { it.height }
            ?.distinctBy { it.height }
            ?: emptyList()
    }

    // Check if formats are loading
    fun isLoadingFormats(videoId: String): Boolean {
        return loadingFormats[videoId] == true
    }

    // Cancel all pending format fetches
    fun cancelAllFormatFetches() {
        fetchJobs.values.forEach { it.cancel() }
        fetchJobs.clear()
        loadingFormats.clear()
    }

    // Cancel specific video format fetch
    fun cancelFormatFetch(videoId: String) {
        fetchJobs[videoId]?.cancel()
        fetchJobs.remove(videoId)
        loadingFormats[videoId] = false
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllFormatFetches()
    }


}
