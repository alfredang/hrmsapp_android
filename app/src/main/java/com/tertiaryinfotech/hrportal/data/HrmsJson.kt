package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.json.Json

/**
 * The one lenient JSON configuration for every backend payload. Lives apart from [Net] (which needs
 * an Android Context) so plain JVM unit tests decode with exactly the settings the app uses.
 */
val HrmsJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
