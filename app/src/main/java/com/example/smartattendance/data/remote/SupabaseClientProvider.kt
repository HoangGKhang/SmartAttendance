package com.example.smartattendance.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://vsistkdrxwnbqpzyilnb.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNoemRxa2hicWhnZnltcGt1a29hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQyNzIyMTYsImV4cCI6MjA4OTg0ODIxNn0.ZIR4GfjVTOU3AmHJh98QWT4gEg5c5p980k-hwesxnAQ"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}