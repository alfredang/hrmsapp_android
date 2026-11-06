package com.tertiaryinfotech.hrportal.data

import android.content.Context
import okhttp3.OkHttpClient

/** Release builds never mock anything. The debug source set holds the real implementation. */
object MockApi {
    fun install(builder: OkHttpClient.Builder, context: Context) = Unit
}
