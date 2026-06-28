package com.gymbuddy.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {
    /** Latest commit(s) touching a given path, newest first. */
    @GET("repos/{repo}/commits")
    suspend fun commits(
        @Path("repo", encoded = true) repo: String,
        @Query("path") path: String,
        @Query("per_page") perPage: Int = 1,
    ): List<CommitDto>
}
