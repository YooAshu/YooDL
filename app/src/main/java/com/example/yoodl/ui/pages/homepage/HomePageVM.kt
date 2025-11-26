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
import com.example.yoodl.BuildConfig
import com.example.yoodl.data.api.RetrofitInstance
import com.example.yoodl.data.models.DownloadQueue
import com.example.yoodl.data.models.DownloadStatus
import com.example.yoodl.data.models.SocialMediaPlatform
import com.example.yoodl.data.models.YtData
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

@HiltViewModel
class HomePageVM @Inject constructor(
) : ViewModel() {

    val sharedUrl = mutableStateOf("")
    var showDownloadSheet by mutableStateOf(false)
    var bottomSheetCurrentVideo by mutableStateOf<YtData?>(null)
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
        platform: String = "youtube",
        onQueue: (DownloadQueue) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val baseDownloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "YooDL/${platform}"
                )

                val downloadDir = if (isAudio) {
                    File(baseDownloadDir, "audio")
                } else {
                    File(baseDownloadDir, "video")
                }
                downloadDir.mkdirs()

                val quality =
                    if (isAudio) "${format?.abr}kbps" else format?.height?.let { "[${it}p]" }
                        ?: "unknown"
                val extension = if (isAudio) "mp3" else format?.ext ?: "mp3"

                val queueItem = DownloadQueue(
                    id = "${videoId}$quality",
                    title = title,
                    url = url,
                    format = format,
                    isAudio = isAudio,
                    status = DownloadStatus.PENDING,
                    progress = 0,
                    eta = 0,
                    filePath = "${downloadDir.absolutePath}/$title-${videoId}$quality.${extension}",
                    thumbnail = thumbnail,
                    formatId = format?.formatId,
                    platform = platform,
                    formatExt = if (isAudio) "mp3" else format?.ext ?: "mp4"
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
                when (identifySocialMediaPlatform(inputUrl)) {
                    SocialMediaPlatform.YouTube -> {
                        if (isValidYouTubeVideoUrl(inputUrl)) {
                            val videoId = extractVideoIdFromUrl(inputUrl)
                            if (videoId == null) {
                                onError(Exception("Invalid video URL"))
                                loadingYTVideosInfo = false
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
                        } else {
                            //handle invalid yt url
                        }
                    }
                    SocialMediaPlatform.Instagram -> {
                        val videoId = extractInstagramId(inputUrl)
                        handleOtherLink(inputUrl,videoId,"instagram", onError)
                    }
                    SocialMediaPlatform.Facebook -> {
                        val videoId = extractFacebookId(inputUrl)
                        handleOtherLink(inputUrl,videoId,"facebook", onError)
                    }
                    SocialMediaPlatform.Unknown -> {
                        val videoId = "unknown_${System.currentTimeMillis()}"
                        handleOtherLink(inputUrl,videoId,"other", onError)
                    }
                }

            }
        }
    }

    fun handleOtherLink(inputUrl: String,videoId: String,platform: String,onError: (Exception) -> Unit) {

        if (videoId == "") {
            onError(Exception("Invalid video URL"))
            loadingYTVideosInfo = false
            return
        }
        // Start fetching
        loadingFormats[videoId] = true
        fetchJobs[videoId] = viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoInfo = YoutubeDL.getInstance().getInfo(inputUrl)

                withContext(Dispatchers.Main) {
                    formatCache[videoId] = videoInfo
                    loadingFormats[videoId] = false
//                    Log.d("Formats", "✅ Formats loaded for $videoId")
                    val entryItem = formatCache[videoId]
//                    Log.d("Formats", "d ${entryItem?.duration}")
                    if (entryItem!=null){
                        ytVideosInfoEntries = listOfNotNull(
                            YtData(
                                id = videoId,
                                title = entryItem.title ?: (platform + videoId),
                                url = inputUrl,
                                thumbnail = entryItem.thumbnail?:"",
                                platform = platform,
                                duration = getFormattedDate(entryItem.duration),
                                channelName = entryItem.uploader?: platform
                            )
                        )
                    }
                    else{
                        onError(Exception("Invalid video URL"))
                    }
                }
            }
            catch (e: Exception) {
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        loadingFormats[videoId] = false
                        Log.e("Formats", "❌ Error loading formats for $videoId: ${e.message}")
                        onError(e)
                    }
                }
            }
            finally {
                lastFetchedPlaylistUrl = inputUrl
                loadingYTVideosInfo = false
            }

        }


    }


    private suspend fun fetchPlaylistEntriesViaAPI(playlistId: String): List<YtData> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.YOUTUBE_API_KEY
                val service = RetrofitInstance.getYouTubeApiService()

                // Fetch playlist name
                val playlistResponse = service.getPlaylistInfo("snippet", playlistId, apiKey)
                playListName = playlistResponse.items.firstOrNull()?.snippet?.title ?: "Unknown"

                // Fetch all video IDs - title - thumbnail - title
                val results = mutableMapOf<String, YtData?>()
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
                        results[videoId] =
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

//                Log.d("PlaylistAPI", "Fetched ${results.size} videos from API")
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
                val apiKey = BuildConfig.YOUTUBE_API_KEY
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
        } catch (_: Exception) {
            null
        }
    }


    private fun extractInstagramId(url: String): String {
        return try {
            val lower = url.lowercase()
            when {
                lower.contains("/p/") -> url.split("/p/")[1].split("/")[0]
                lower.contains("/reel/") -> url.split("/reel/")[1].split("/")[0]
                lower.contains("/tv/") -> url.split("/tv/")[1].split("/")[0]
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractFacebookId(url: String): String {
        return try {
            val lower = url.lowercase()
            when {
                lower.contains("watch?v=") -> url.split("watch?v=")[1].split("&")[0]
                lower.contains("/videos/") -> url.split("/videos/")[1].split("/")[0]
                lower.contains("/share/r") ->url.split("/share/r/")[1].split("/")[0]
                else -> ""
            }
        } catch (_: Exception) {
            ""
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
        } catch (_: Exception) {
            ""
        }
    }

    fun identifySocialMediaPlatform(url: String): SocialMediaPlatform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> SocialMediaPlatform.YouTube
            lower.contains("instagram.com") || lower.contains("instagr.am") -> SocialMediaPlatform.Instagram
            lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> SocialMediaPlatform.Facebook
            else -> SocialMediaPlatform.Unknown
        }
    }

    fun fetchFormatsForVideo(videoId: String, videoUrl: String) {
        // If already cached, return immediately
        if (formatCache.containsKey(videoId)) {
//            Log.d("Formats", "Using cached formats for $videoId")
            return
        }

        // If already loading, don't start again
        if (loadingFormats[videoId] == true) {
//            Log.d("Formats", "Already loading formats for $videoId")
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
//                    Log.d("Formats", "✅ Formats loaded for $videoId")
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
    fun getVideoFormats(videoId: String): List<VideoFormat> {
        return formatCache[videoId]?.formats
            ?.filter { format ->
                format.vcodec != null &&
                        format.vcodec != "none" && format.height > 0
            }
            ?.sortedByDescending { it.height }
            ?.distinctBy { it.height }
            ?: emptyList()
    }

    fun getAudioFormats(videoId: String): List<VideoFormat> {
        return formatCache[videoId]?.formats
            ?.filter { format ->
                format.acodec != null &&
                        format.acodec != "none" &&
                        format.vcodec == null || format.vcodec == "none"
            }
            ?.sortedByDescending { it.abr }
            ?.distinctBy { it.abr }
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
//    fun cancelFormatFetch(videoId: String) {
//        fetchJobs[videoId]?.cancel()
//        fetchJobs.remove(videoId)
//        loadingFormats[videoId] = false
//    }

    override fun onCleared() {
        super.onCleared()
        cancelAllFormatFetches()
    }

    // kotlin
    fun getFormattedDate(timestamp: Int): String {
        val secondsTotal = if (timestamp < 0) 0 else timestamp
        val hours = secondsTotal / 3600
        val minutes = (secondsTotal % 3600) / 60
        val seconds = secondsTotal % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }


}
