package com.gymbuddy.media

import com.gymbuddy.BuildConfig

/** Builds an absolute raw-GitHub URL from a dataset-relative media path. */
fun mediaUrl(relativePath: String): String {
    if (relativePath.isBlank()) return ""
    if (relativePath.startsWith("http")) return relativePath
    return BuildConfig.DATASET_RAW_BASE + relativePath.trimStart('/')
}
