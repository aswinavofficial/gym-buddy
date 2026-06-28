package com.gymbuddy.data.remote

import com.gymbuddy.BuildConfig
import com.gymbuddy.data.AppJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request

/** Reads the upstream dataset + version metadata from GitHub. */
class DatasetRemoteSource(
    private val api: GitHubApi,
    private val client: OkHttpClient,
) {
    /** Returns the latest commit sha for the dataset file, or null if unreachable. */
    suspend fun latestSha(): RemoteVersion? = withContext(Dispatchers.IO) {
        runCatching {
            val commit = api.commits(BuildConfig.DATASET_REPO, "data/exercises.json").firstOrNull()
            commit?.let { RemoteVersion(it.sha, it.commit.committer.date) }
        }.getOrNull()
    }

    /** Streams and parses the latest `exercises.json`. */
    suspend fun fetchExercises(): List<ExerciseDto> = withContext(Dispatchers.IO) {
        val url = BuildConfig.DATASET_RAW_BASE + "data/exercises.json"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            AppJson.decodeFromString(ListSerializer(ExerciseDto.serializer()), body)
        }
    }
}

data class RemoteVersion(val sha: String, val date: String)
