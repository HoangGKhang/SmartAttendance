
package com.example.smartattendance.data.remote

import com.example.smartattendance.data.remote.dto.MoodleLoginRequestDto
import com.example.smartattendance.data.remote.dto.MoodleLoginResponseDto

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions

import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class MoodleAuthApiClient(
    private val supabase: SupabaseClient =
        SupabaseProvider.client
) {

    suspend fun login(
        username: String,
        password: String
    ): MoodleLoginResponseDto {

        val request = MoodleLoginRequestDto(
            username = username.trim(),
            password = password
        )

        val response = supabase.functions.invoke(
            function = "auth-moodle",
            body = request,
            headers = Headers.build {
                append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            }
        )

        return response.body<MoodleLoginResponseDto>()
    }
}
