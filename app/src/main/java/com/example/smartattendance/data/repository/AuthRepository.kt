package com.example.smartattendance.data.repository

import com.example.smartattendance.data.remote.SupabaseProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlin.coroutines.cancellation.CancellationException

class AuthRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {
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

    suspend fun logout(): Result<Unit> {
        return try {

            supabase.auth.signOut()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
