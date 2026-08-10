package com.smartteacher.app.notification

import com.smartteacher.app.backend.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Triggers server-side push notifications.
 *
 * The teacher app calls [trigger] after creating an assignment/exam/note.
 * This sends an HTTP request to a Supabase Edge Function which looks up the
 * FCM tokens of every student in that class/section and dispatches a real
 * Firebase Cloud Messaging push to all of them.
 *
 * The Edge Function source is included in supabase/edge/send_push.ts and
 * its deploy command is in README.md.
 */
object NotificationTrigger {

    @Serializable
    private data class PushRequest(
        val grade: String,
        val section: String,
        val type: String,
        val title: String,
        val body: String
    )

    private val client by lazy {
        HttpClient(Android) {
            engine { connectTimeout = 15_000; socketTimeout = 15_000 }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
    }

    /**
     * Fire-and-forget push notification request to the server.
     * Returns true if the server accepted the request.
     */
    suspend fun trigger(
        grade: String,
        section: String,
        type: String,   // "assignment" | "exam" | "note"
        title: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) return@withContext false
        val url = "${SupabaseConfig.getEdgeFunctionBase()}/send-push"
        val payload = PushRequest(grade, section, type, title, body)
        runCatching {
            val resp: HttpResponse = client.post(url) { setBody(payload) }
            resp.status.value in 200..299
        }.getOrDefault(false)
    }
}
