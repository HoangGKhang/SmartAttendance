package com.example.smartattendance.data.repository

import com.example.smartattendance.data.model.Profile
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.ProfileDto
import com.example.smartattendance.data.remote.dto.toProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProfileRepository(
    private val supabase: SupabaseClient =
        SupabaseProvider.client
) {

    suspend fun getProfile(
        userId: String
    ): Result<Profile> {

        return try {

            val profileDto = supabase
                .from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()

            Result.success(
                profileDto.toProfile()
            )

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}