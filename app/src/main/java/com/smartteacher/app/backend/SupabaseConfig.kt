package com.smartteacher.app.backend

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

/**
 * Central Supabase configuration.
 *
 * The project is wired to a real cloud backend (Supabase) which provides:
 *  - Postgres database (Postgrest) for all data
 *  - GoTrue auth for secure authentication
 *  - Realtime websocket channel for live synchronization across devices
 *
 * IMPORTANT: Before building the APK, replace SUPABASE_URL and SUPABASE_ANON_KEY
 * with the credentials from your Supabase project dashboard
 * (Project Settings -> API -> URL and anon public key).
 *
 * Detailed setup steps are in README.md and supabase/schema.sql
 */
object SupabaseConfig {

    // ============================================================
    //  REPLACE THESE WITH YOUR REAL SUPABASE CREDENTIALS
    // ============================================================
    private const val SUPABASE_URL = "https://YOUR-PROJECT-REF.supabase.co"
    private const val SUPABASE_ANON_KEY = "YOUR-ANON-PUBLIC-KEY"

    // ============================================================

    private lateinit var client: SupabaseClient

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    fun get(): SupabaseClient = client

    fun auth(): Auth = client.auth

    fun isConfigured(): Boolean =
        !SUPABASE_URL.contains("YOUR-PROJECT-REF") &&
        !SUPABASE_ANON_KEY.contains("YOUR-ANON-PUBLIC-KEY")

    /** Base URL used to call Supabase Edge Functions (for push notifications). */
    fun getEdgeFunctionBase(): String =
        SUPABASE_URL.replace(".co", ".co/functions/v1")
}
