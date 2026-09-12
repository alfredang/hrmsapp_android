package com.tertiaryinfotech.hrportal.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tertiaryinfotech.hrportal.data.ApiException
import com.tertiaryinfotech.hrportal.data.AuthException
import kotlinx.coroutines.launch

/**
 * Standard data screen: kicks off [fetch] on first appearance, shows a spinner / error+retry,
 * and once loaded renders a pull-to-refresh [LazyColumn] whose items come from [body].
 * Mirrors the iOS `AsyncContent { ScrollView { … }.refreshable { … } }` pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AsyncListScreen(
    fetch: suspend () -> T,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    key: Any? = Unit,
    body: LazyListScope.(T) -> Unit,
) {
    var state by remember { mutableStateOf<LoadState<T>>(LoadState.Idle) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val load: suspend () -> Unit = {
        state = LoadState.Loading
        state = runFetch(fetch)
    }

    // Refetch when the caller's query input changes (e.g. the team calendar's year as the user
    // pages across a year boundary). `key` defaults to Unit, so callers that fetch once are
    // unaffected. The initial load still comes from AsyncContent's own first-appearance effect;
    // this only handles subsequent key changes, hence the Idle skip.
    var lastKey by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (key != lastKey) {
            lastKey = key
            load()
        }
    }

    AsyncContent(state = state, load = load) { data ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    state = runFetch(fetch)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                body(data)
            }
        }
    }
}

private suspend fun <T> runFetch(fetch: suspend () -> T): LoadState<T> = try {
    LoadState.Loaded(fetch())
} catch (e: AuthException) {
    LoadState.Failed(e.message ?: "Could not load.")
} catch (e: ApiException) {
    LoadState.Failed(e.message ?: "Could not load.")
} catch (e: Exception) {
    LoadState.Failed("Could not load.")
}
