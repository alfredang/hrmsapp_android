package com.tertiaryinfotech.hrportal

import android.app.Application
import com.tertiaryinfotech.hrportal.data.Net

/** Initialises the shared networking stack (OkHttp + persistent cookie jar) once per process. */
class TertiaryHrmsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Net.init(this)
    }
}
