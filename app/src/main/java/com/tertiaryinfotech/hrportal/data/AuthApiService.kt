package com.tertiaryinfotech.hrportal.data

import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class CsrfResponse(val csrfToken: String)

@Serializable
data class SendOtpBody(val email: String)

/** Mirrors `POST /api/auth/send-otp`'s real success body exactly (`{success:true, message:"OTP
 *  has been sent to your email address"}`) so the UI can show the server's actual copy instead of
 *  a client-guessed string. */
@Serializable
data class SendOtpResponse(val success: Boolean = true, val message: String? = null)

/**
 * Retrofit definition of the NextAuth (Auth.js) endpoints `AuthService` drives — the standard
 * csrf → callback → session dance, plus send-otp and signout. Mirrors the shape of
 * `HrmsApiService`: every method returns `Response<T>` so the wrapper can inspect status codes
 * and the server's `{"error": "..."}` body on failure.
 */
interface AuthApiService {

    @GET("api/auth/csrf")
    suspend fun csrf(): Response<CsrfResponse>

    @FormUrlEncoded
    @POST("api/auth/callback/{provider}")
    suspend fun callback(@Path("provider") provider: String, @FieldMap fields: Map<String, String>): Response<ResponseBody>

    // Raw body: the endpoint returns the literal text "null" for no session, which a
    // non-nullable SessionResponse converter can't decode — AuthService parses it manually.
    @GET("api/auth/session")
    suspend fun session(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api/auth/signout")
    suspend fun signOut(@FieldMap fields: Map<String, String>): Response<ResponseBody>

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body body: SendOtpBody): Response<SendOtpResponse>

    /** Exchanges a Google `id_token` for the standard NextAuth session cookie. The server
     *  verifies the token with Google and checks its audience allow-list — the client is never
     *  trusted on identity. Pre-existing route, already used by the iOS app. */
    @POST("api/auth/google-mobile")
    suspend fun googleMobile(@Body body: GoogleMobileBody): Response<ResponseBody>
}
