package com.soulmatch.app.di
import com.soulmatch.app.BuildConfig
import com.soulmatch.app.data.api.*
import com.soulmatch.app.data.local.UserPreferences
import com.soulmatch.app.data.models.AuthData
import com.soulmatch.app.data.models.GenericResponse
import com.soulmatch.app.util.CrashReporter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(String::class.java, object : TypeAdapter<String>() {
            override fun write(out: JsonWriter, value: String?) {
                out.value(value ?: "")
            }

            override fun read(reader: JsonReader): String {
                return if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    ""
                } else {
                    reader.nextString()
                }
            }
        })
        .create()

    private fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun refreshAccessToken(prefs: UserPreferences): AuthData? {
        val refreshToken = prefs.currentRefreshToken() ?: return null
        val refreshBody = gson.toJson(mapOf("refreshToken" to refreshToken)).toRequestBody("application/json".toMediaTypeOrNull())
        val request = okhttp3.Request.Builder()
            .url("${BuildConfig.AUTH_BASE_URL}auth/refresh-token")
            .post(refreshBody)
            .addHeader("Content-Type", "application/json")
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            val type = object : TypeToken<GenericResponse<AuthData>>() {}.type
            val payload: GenericResponse<AuthData> = gson.fromJson(body, type) ?: return null
            val auth = payload.data ?: return null
            runBlocking {
                prefs.saveAuthToken(auth.accessToken)
                prefs.saveRefreshToken(auth.refreshToken)
                prefs.saveUserId(auth.userId)
            }
            return auth
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var current = response.priorResponse
        while (current != null) {
            result += 1
            current = current.priorResponse
        }
        return result
    }

    @Provides @Singleton
    fun provideOkHttpClient(prefs: UserPreferences): OkHttpClient {
        val auth = Interceptor { chain ->
            val token = prefs.currentAuthToken()
            val builder = chain.request().newBuilder()
                .addHeader("x-device-id", prefs.installationId())
            if (!token.isNullOrEmpty()) builder.addHeader("Authorization","Bearer $token")
            val req = builder.build()
            chain.proceed(req)
        }
        val authenticator = Authenticator { _: Route?, response: Response ->
            if (response.request.url.encodedPath.endsWith("/auth/refresh-token")) return@Authenticator null
            if (responseCount(response) >= 2) return@Authenticator null
            val refreshed = refreshAccessToken(prefs) ?: return@Authenticator null
            response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
        val log = HttpLoggingInterceptor().apply { level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE }
        val reliability = Interceptor { chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (response.code >= 500) {
                    CrashReporter.breadcrumb("http_${response.code}:${request.url.encodedPath}")
                }
                response
            } catch (error: Exception) {
                CrashReporter.recordNonFatal(error, "network:${request.url.encodedPath}")
                throw error
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(reliability)
            .addInterceptor(log)
            .authenticator(authenticator)
            .connectTimeout(30,TimeUnit.SECONDS)
            .readTimeout(30,TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton @Named("auth")
    fun provideAuthRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.AUTH_BASE_URL, client)

    @Provides @Singleton @Named("profile")
    fun provideProfileRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.PROFILE_BASE_URL, client)

    @Provides @Singleton @Named("matching")
    fun provideMatchingRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.MATCHING_BASE_URL, client)

    @Provides @Singleton @Named("search")
    fun provideSearchRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.SEARCH_BASE_URL, client)

    @Provides @Singleton @Named("chat")
    fun provideChatRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.CHAT_BASE_URL, client)

    @Provides @Singleton @Named("payment")
    fun providePaymentRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.PAYMENT_BASE_URL, client)

    @Provides @Singleton @Named("notification")
    fun provideNotificationRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.NOTIFICATION_BASE_URL, client)

    @Provides @Singleton @Named("controlPlane")
    fun provideControlPlaneRetrofit(client: OkHttpClient): Retrofit = buildRetrofit(BuildConfig.CONTROL_PLANE_BASE_URL, client)

    @Provides @Singleton
    fun provideAuthApi(@Named("auth") retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides @Singleton
    fun provideProfileApi(@Named("profile") retrofit: Retrofit): ProfileApiService = retrofit.create(ProfileApiService::class.java)

    @Provides @Singleton
    fun provideMatchingApi(@Named("matching") retrofit: Retrofit): MatchingApiService = retrofit.create(MatchingApiService::class.java)

    @Provides @Singleton
    fun provideSearchApi(@Named("search") retrofit: Retrofit): SearchApiService = retrofit.create(SearchApiService::class.java)

    @Provides @Singleton
    fun provideInterestApi(@Named("matching") retrofit: Retrofit): InterestApiService = retrofit.create(InterestApiService::class.java)

    @Provides @Singleton
    fun providePaymentApi(@Named("payment") retrofit: Retrofit): PaymentApiService = retrofit.create(PaymentApiService::class.java)

    @Provides @Singleton
    fun provideNotificationApi(@Named("notification") retrofit: Retrofit): NotificationApiService = retrofit.create(NotificationApiService::class.java)

    @Provides @Singleton
    fun provideControlPlaneApi(@Named("controlPlane") retrofit: Retrofit): ControlPlaneApiService = retrofit.create(ControlPlaneApiService::class.java)

    @Provides @Singleton
    fun provideChatApi(@Named("chat") retrofit: Retrofit): ChatApiService = retrofit.create(ChatApiService::class.java)
}
