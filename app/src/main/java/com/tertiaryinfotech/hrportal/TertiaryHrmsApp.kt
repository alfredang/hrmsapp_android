package com.tertiaryinfotech.hrportal

import android.app.Application
import com.tertiaryinfotech.hrportal.data.Net
import dagger.hilt.android.HiltAndroidApp

/** Initialises the shared networking stack (OkHttp + persistent cookie jar) once per process,
 *  and roots the Hilt dependency graph for the app. */
@HiltAndroidApp
class TertiaryHrmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Net.init(this)
    }
}
