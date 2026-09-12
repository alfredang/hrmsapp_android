package com.tertiaryinfotech.hrportal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tertiaryinfotech.hrportal.data.GoogleSignInService

/**
 * Catches Google's OAuth redirect back into the app (the reversed-client-id scheme declared in
 * the manifest) and hands the callback URL to [GoogleSignInService], which is suspended waiting
 * for it.
 *
 * Declared `noHistory` + `launchMode=singleTask` and finished immediately, so it never appears in
 * the back stack: the Custom Tab closes and the user lands back on the login screen exactly where
 * they left it, rather than on a blank activity.
 */
class GoogleAuthRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.data?.let { GoogleSignInService.onRedirect(it) }
        finish()
    }
}
