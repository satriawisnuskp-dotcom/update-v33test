package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GoogleAuthManager(
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        private const val TAG = "GoogleAuthManager"
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Silent auto sign-in using GetGoogleIdOption in its own GetCredentialRequest.
     */
    suspend fun trySilentSignIn(activityContext: Context): Result<FirebaseUser?> {
        if (auth.currentUser != null) {
            return Result.success(auth.currentUser)
        }
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val user = handleGoogleCredential(result.credential)
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            Result.success(null)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google Sign-In cancelled", e)
            Result.success(null)
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google Sign-In unavailable: ${e.message}")
            Result.success(null)
        }
    }

    /**
     * Interactive Google Sign-In using GetSignInWithGoogleOption in its own GetCredentialRequest.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        return try {
            val webClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val user = handleGoogleCredential(result.credential)
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled by user or play services config", e)
            Result.failure(Exception("Login dibatalkan. Pilih akun Google untuk melanjutkan."))
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential found on device", e)
            Result.failure(Exception("Tidak ada akun Google yang tersedia di perangkat ini."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(Exception(e.localizedMessage ?: "Gagal membuka dialog Login Google."))
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Invalid Google ID token", e)
            Result.failure(Exception("Token Google tidak valid."))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Google sign-in failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Gagal masuk dengan akun Google."))
        }
    }

    private suspend fun handleGoogleCredential(credential: androidx.credentials.Credential): FirebaseUser {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            return authResult.user
                ?: throw IllegalStateException("Autentikasi Firebase gagal menghasilkan pengguna.")
        }
        throw IllegalStateException("Tipe kredensial tidak didukung: ${credential.type}")
    }

    suspend fun signOut(
        context: Context,
        beforeAuthSignOut: suspend () -> Unit = {}
    ) {
        withContext(NonCancellable) {
            try {
                beforeAuthSignOut()
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup before signOut failed: ${e.message}")
            }
            try {
                auth.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth signOut error: ${e.message}")
            }
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "ClearCredentialState error: ${e.message}")
            }
        }
    }
}
