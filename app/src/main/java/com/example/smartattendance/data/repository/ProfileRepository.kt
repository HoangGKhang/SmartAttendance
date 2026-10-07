package com.example.smartattendance.data.repository

import com.example.smartattendance.data.model.Profile
import com.example.smartattendance.data.remote.SupabaseProvider
import com.example.smartattendance.data.remote.dto.ProfileDto
import com.example.smartattendance.data.remote.dto.toProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.coroutines.cancellation.CancellationException

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
                .decodeSingleOrNull<ProfileDto>()
                ?: return Result.failure(
                    IllegalStateException(
                        "Không tìm thấy hồ sơ người dùng. " +
                            "Vui lòng kiểm tra dữ liệu profiles và quyền RLS."
                    )
                )

            Result.success(
                profileDto.toProfile()
            )

        } catch (exception: CancellationException) {
            throw exception
        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
