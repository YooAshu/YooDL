package com.example.yoodl.data.models

data class PlaylistResponse(
    val items: List<PlaylistItem>
)

data class PlaylistItem(
    val snippet: PlaylistSnippet?
)

data class PlaylistSnippet(
    val title: String?
)

data class PlaylistItemsResponse(
    val items: List<PlaylistItemDetail>,
    val nextPageToken: String?
)

data class PlaylistItemDetail(
    val contentDetails: ContentDetails?,
    val snippet: ItemSnippet?
)

data class ContentDetails(
    val videoId: String?
)

data class ItemSnippet(
    val title: String?,
    val channelTitle: String?,
    val thumbnails: Thumbnails?
)

data class Thumbnails(
    val maxres: Thumbnail?,
    val high: Thumbnail?
)

data class Thumbnail(
    val url: String?
)

data class VideosResponse(
    val items: List<VideoItem>
)

data class VideoItem(
    val id: String,
    val snippet: ItemSnippet?,
    val contentDetails: VideoContentDetails?
)

data class VideoContentDetails(
    val duration: String?
)
