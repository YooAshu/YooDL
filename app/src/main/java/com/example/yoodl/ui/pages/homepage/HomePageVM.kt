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
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.data.models.YtData
import com.example.yoodl.data.repository.DownloadRepository
import com.example.yoodl.ui.pages.downloads.DownloadPageVM
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.compareTo
import kotlin.coroutines.cancellation.CancellationException
import kotlin.sequences.ifEmpty
import kotlin.text.ifEmpty
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
    var loadingInfo by mutableStateOf(false)
    var streamInfo by mutableStateOf<VideoInfo?>(null)
    var error by mutableStateOf<String?>(null)
    private var infoJob: Job? = null
    var availableFormats by mutableStateOf<List<VideoFormat>>(emptyList())
    var showDownloadSheet by mutableStateOf(false)
    var showDownloadSheetPL by mutableStateOf(false)
    var bottomSheetPLId by mutableStateOf("")
    var bottomSheetPLUrl by mutableStateOf("")
    var lastFetchedPlaylistUrl by mutableStateOf("")

    var playlistEntries by mutableStateOf<List<YtData>>(emptyList())
    var loadingPlaylist by mutableStateOf(false)
    var playListName by mutableStateOf("")

    // Cache of fetched formats (videoId -> VideoInfo)
     val formatCache = mutableStateMapOf<String, VideoInfo?>()

    // Track which videos are currently loading formats
     val loadingFormats = mutableStateMapOf<String, Boolean>()

    // Active jobs for each video (to cancel when needed)
    private val fetchJobs = mutableMapOf<String, Job>()


    fun getYTVideoInfo(url: String, onError: (Exception) -> Unit = {}, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            // Cancel previous requests
            infoJob?.cancel()

            // Reset state
            streamInfo = null
            error = null

            try {
                // Start both operations concurrently
                val deferred = async(Dispatchers.IO) {
                    loadingInfo = true
                    YoutubeDL.getInstance().getInfo(url)
                }

                // Await info first so UI updates fast
                streamInfo = deferred.await()
                loadingInfo = false
                onSuccess()
                loadVideoFormats()


            } catch (e: Exception) {
                if (e !is CancellationException) {
                    error = e.message
                    loadingInfo = false
                    onError(e)
                }
            }
        }
    }


    private fun loadVideoFormats() {
        availableFormats = streamInfo?.formats
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

    fun downloadFormat(
        url: String,
        videoId:String="",
        title: String="Download",
        thumbnail: String="",
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



    fun handleSharedOrTypedUrl(
        inputUrl: String,
        onError: (Exception) -> Unit = {},
        onSuccessPerVideo: (String) -> Unit = {}
    ) {
        if (inputUrl.isBlank()) return

        // Skip if same URL was already successfully fetched
        if (inputUrl == lastFetchedPlaylistUrl && playlistEntries.isNotEmpty()) {
            return
        }

        viewModelScope.launch {
            if (isPlaylistUrl(inputUrl)) {
                loadingPlaylist = true
                playlistEntries = emptyList()

                val playlistId = extractPlaylistId(inputUrl)
                if (playlistId.isBlank()) {
                    onError(Exception("Invalid playlist URL"))
                    loadingPlaylist = false
                    return@launch
                }

                val entries = try {
                    fetchPlaylistEntriesViaAPI(playlistId)
                } catch (e: Exception) {
                    Log.e("HandleURL", "Error: ${e.message}", e)
                    onError(e)
                    emptyList()
                }

                playlistEntries = entries
                lastFetchedPlaylistUrl = inputUrl
                loadingPlaylist = false

                if (entries.isEmpty()) {
                    onError(Exception("No playlist entries found"))
                }
            }
        }
    }

    private suspend fun fetchPlaylistEntriesViaAPI(playlistId: String): List<YtData> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<YtData>()
                val videoIds = mutableListOf<String>()
                var pageToken = ""
                val apiKey = "AIzaSyDKmZ1uGGBCZcv04dQ9F-aiLKvfQEfAyOw"

                // Fetch playlist name
                val playlistUrl = "https://www.googleapis.com/youtube/v3/playlists?" +
                        "part=snippet&" +
                        "id=$playlistId&" +
                        "key=$apiKey"

                val playlistConnection = java.net.URL(playlistUrl).openConnection() as java.net.HttpURLConnection
                playlistConnection.requestMethod = "GET"
                val playlistResponse = playlistConnection.inputStream.bufferedReader().use { it.readText() }
                val playlistJson = JSONObject(playlistResponse)
                val playlistItems = playlistJson.optJSONArray("items")
                if (playlistItems != null && playlistItems.length() > 0) {
                    playListName = playlistItems.getJSONObject(0).optJSONObject("snippet")?.optString("title") ?: "Unknown"
                }


                // Collect all video IDs first
                while (true) {
                    val urlString = "https://www.googleapis.com/youtube/v3/playlistItems?" +
                            "part=contentDetails,snippet&" +
                            "playlistId=$playlistId&" +
                            "maxResults=50&" +
                            "key=$apiKey" +
                            (if (pageToken.isNotEmpty()) "&pageToken=$pageToken" else "")

                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items") ?: break

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val videoId = item.optJSONObject("contentDetails")?.optString("videoId") ?: continue
                        videoIds.add(videoId)
                    }

                    pageToken = json.optString("nextPageToken")
                    if (pageToken.isEmpty()) break
                }

                // Get durations in batches of 50
                val durationMap = mutableMapOf<String, String>()
                for (i in videoIds.indices step 50) {
                    val ids = videoIds.subList(i, minOf(i + 50, videoIds.size)).joinToString(",")
                    val videosUrl = "https://www.googleapis.com/youtube/v3/videos?" +
                            "part=contentDetails&" +
                            "id=$ids&" +
                            "key=$apiKey"

                    val url = java.net.URL(videosUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val videos = json.optJSONArray("items") ?: continue

                    for (j in 0 until videos.length()) {
                        val video = videos.getJSONObject(j)
                        val id = video.optString("id")
                        val duration = video.optJSONObject("contentDetails")?.optString("duration") ?: ""
                        durationMap[id] = duration
                    }
                }

                // Final pass: combine all data
                pageToken = ""
                while (true) {
                    val urlString = "https://www.googleapis.com/youtube/v3/playlistItems?" +
                            "part=contentDetails,snippet&" +
                            "playlistId=$playlistId&" +
                            "maxResults=50&" +
                            "key=$apiKey" +
                            (if (pageToken.isNotEmpty()) "&pageToken=$pageToken" else "")

                    val url = java.net.URL(urlString)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items") ?: break

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val videoId = item.optJSONObject("contentDetails")?.optString("videoId") ?: continue
                        val title = item.optJSONObject("snippet")?.optString("title") ?: "Unknown"
                        val channelName = item.optJSONObject("snippet")?.optString("channelTitle") ?: "Unknown"
                        val duration = durationMap[videoId] ?: ""
                        val thumbnailUrl = item.optJSONObject("snippet")
                            ?.optJSONObject("thumbnails")
                            ?.optJSONObject("maxres")
                            ?.optString("url")
                            ?: item.optJSONObject("snippet")
                                ?.optJSONObject("thumbnails")
                                ?.optJSONObject("high")
                                ?.optString("url")
                            ?: ""

                        val ytItem = YtData(
                            id = videoId,
                            title = title,
                            url = "https://www.youtube.com/watch?v=$videoId",
                            thumbnail = thumbnailUrl,
                            duration = duration,
                            channelName = channelName
                        )
                        results.add(ytItem)
                    }

                    pageToken = json.optString("nextPageToken")
                    if (pageToken.isEmpty()) break
                }

                Log.d("PlaylistAPI", "Fetched ${results.size} videos from API")
                results
            } catch (e: Exception) {
                Log.e("PlaylistAPI", "Error: ${e.message}", e)
                emptyList()
            }
        }
    }


    private fun extractPlaylistId(url: String): String {
        return try {
            val listParam = url.split("list=")[1].split("&")[0]
            listParam
        } catch (e: Exception) {
            ""
        }
    }

    fun fetchFormatsForVideo(videoId: String, videoUrl: String){
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
