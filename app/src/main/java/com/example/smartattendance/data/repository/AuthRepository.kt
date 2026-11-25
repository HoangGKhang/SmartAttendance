package com.example.smartattendance.data.repository

import com.example.smartattendance.data.remote.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlin.coroutines.cancellation.CancellationException

import com.example.smartattendance.data.remote.MoodleAuthApiClient
import io.github.jan.supabase.auth.user.UserSession

class AuthRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private val moodleAuthApiClient =
        MoodleAuthApiClient(supabase)

    suspend fun login(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    suspend fun loginWithMoodle(
        username: String,
        password: String
    ): Result<Unit> {

        return try {

            // 1. Authenticate through the Moodle bridge.
            val response = moodleAuthApiClient.login(
                username = username,
                password = password
            )

            // 2. Validate the response.
            if (!response.success) {
                throw IllegalStateException(
                    "Đăng nhập Moodle thất bại"
                )
            }

            val session = response.session
                ?: throw IllegalStateException(
                    "Backend không trả về Supabase Session"
                )

            val expectedUserId =
                response.user?.supabaseUserId
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException(
                        "Không tìm thấy danh tính Supabase"
                    )

            if (
                session.accessToken.isBlank() ||
                session.refreshToken.isBlank()
            ) {
                throw IllegalStateException(
                    "Access Token hoặc Refresh Token không hợp lệ"
                )
            }

            // 3. Import the session into Supabase Auth SDK.
            val authUser = supabase.auth.retrieveUser(session.accessToken)

            if (authUser.id != expectedUserId) {
                throw IllegalStateException("Phiên đăng nhập không khớp danh tính")
            }

            require(session.expiresIn > 0) {
                "Thời hạn phiên đăng nhập không hợp lệ"
            }

            supabase.auth.importSession(
                UserSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    expiresIn = session.expiresIn.toLong(),
                    tokenType = "Bearer",
                    user = authUser
                )
            )

            // 5. Login completed.
            Result.success(Unit)

        } catch (exception: CancellationException) {
            throw exception

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {

            supabase.auth.signOut()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
