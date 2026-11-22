package com.tertiaryinfotech.hrportal.data

/**
 * The first login screen's default per email (Alfred, 22 Nov 2026; iOS `AuthViewModel.usesGoogleByDefault`): a
 * company address (@tertiaryinfotech.com) signs in with Google when this build has a Google client id; any other
 * address (interns, part-time staff) is emailed a one-time code. A password is the last option. Pure, unit-tested.
 */
object LoginDefaults {
    fun usesGoogle(email: String?, googleConfigured: Boolean): Boolean =
        googleConfigured && GoogleSignInService.isCompanyEmail(email)

    /** "Welcome back" once an email is filled in (typed or remembered), else "Sign in". */
    fun title(email: String?): String = if (email.isNullOrBlank()) "Sign in" else "Welcome back"

    const val SUBTITLE = "Enter your email. Staff sign in with Google, interns and part-time staff with a code."
}
