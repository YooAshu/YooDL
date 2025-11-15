package com.example.yoodl.data.api

import com.example.yoodl.data.models.PlaylistItemsResponse
import com.example.yoodl.data.models.PlaylistResponse
import com.example.yoodl.data.models.VideosResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("playlists")
    suspend fun getPlaylistInfo(
        @Query("part") part: String,
        @Query("id") id: String,
        @Query("key") key: String
    ): PlaylistResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String = "",
        @Query("key") key: String
    ): PlaylistItemsResponse

    @GET("videos")
    suspend fun getVideoDurations(
        @Query("part") part: String,
        @Query("id") ids: String,
        @Query("key") key: String
    ): VideosResponse

    @GET("videos")
    suspend fun getSingleVideoInfo(
        @Query("part") part: String,
        @Query("id") id: String,
        @Query("key") key: String
    ): VideosResponse

}
