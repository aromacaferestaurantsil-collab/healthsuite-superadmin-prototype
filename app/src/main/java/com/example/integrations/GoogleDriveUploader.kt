package com.example.integrations

import android.accounts.Account
import android.app.Activity
import android.app.PendingIntent
import com.example.model.DriveUploadResult
import com.example.security.AdminIdentityPolicy
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface DriveAuthorizationOutcome {
    data class Authorized(val accessToken: String) : DriveAuthorizationOutcome
    data class ResolutionRequired(val pendingIntent: PendingIntent) : DriveAuthorizationOutcome
    data class Failed(val message: String) : DriveAuthorizationOutcome
}

class GoogleDriveUploader(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    suspend fun requestDriveAuthorization(
        activity: Activity,
        signedInEmail: String
    ): DriveAuthorizationOutcome {
        val cleanEmail = signedInEmail.trim()
        if (cleanEmail.isBlank()) {
            return DriveAuthorizationOutcome.Failed(
                "Sign in with your Google account first. Each user must use their own Google Drive."
            )
        }
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .setAccount(Account(cleanEmail, "com.google"))
                .build()
            val result = Identity.getAuthorizationClient(activity).authorize(request).await()
            val pendingIntent = result.pendingIntent
            if (result.hasResolution() && pendingIntent != null) {
                DriveAuthorizationOutcome.ResolutionRequired(pendingIntent)
            } else {
                val token = result.accessToken
                if (token.isNullOrBlank()) {
                    DriveAuthorizationOutcome.Failed("Google Drive authorization did not return an access token.")
                } else {
                    DriveAuthorizationOutcome.Authorized(token)
                }
            }
        } catch (e: Exception) {
            DriveAuthorizationOutcome.Failed(e.localizedMessage ?: "Failed to authorize Google Drive")
        }
    }

    suspend fun uploadCsvToDrive(
        accessToken: String,
        expectedUserEmail: String,
        fileName: String,
        csvContent: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        runCatching {
            val driveOwnerEmail = fetchDriveOwnerEmail(accessToken)
            val validationError = AdminIdentityPolicy.validateDriveOwnerForSignedInUser(
                signedInEmail = expectedUserEmail,
                driveOwnerEmail = driveOwnerEmail
            )
            if (validationError != null) {
                throw SecurityException(validationError)
            }

            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "text/csv")
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addPart(csvContent.toRequestBody("text/csv; charset=UTF-8".toMediaType()))
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,webViewLink")
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = runCatching {
                        JSONObject(rawBody).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    throw IllegalStateException(
                        errorMsg?.takeIf { it.isNotBlank() }
                            ?: "Google Drive upload failed (HTTP ${response.code})"
                    )
                }
                val json = JSONObject(rawBody)
                DriveUploadResult(
                    fileId = json.getString("id"),
                    fileName = json.optString("name", fileName),
                    webViewLink = json.optString("webViewLink").takeIf { it.isNotBlank() },
                    uploadedAtMillis = System.currentTimeMillis(),
                    ownerEmail = driveOwnerEmail
                )
            }
        }
    }

    private fun fetchDriveOwnerEmail(accessToken: String): String {
        val aboutReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/about?fields=user(emailAddress,displayName)")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(aboutReq).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SecurityException("Unable to verify Google Drive account ownership (HTTP ${response.code}).")
            }
            val userObj = JSONObject(raw).optJSONObject("user")
            return userObj?.optString("emailAddress").orEmpty().trim()
        }
    }

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
