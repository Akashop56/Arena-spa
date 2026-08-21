package com.ronin.ai.core.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ronin.ai.BuildConfig
import com.ronin.ai.core.ai.providers.AiApi
import com.ronin.ai.core.common.Constants
import com.ronin.ai.core.data.db.RoninDatabase
import com.ronin.ai.core.data.db.dao.ConversationDao
import com.ronin.ai.core.data.db.dao.ExperienceDao
import com.ronin.ai.core.data.db.dao.MemoryDao
import com.ronin.ai.core.data.db.dao.NotificationEventDao
import com.ronin.ai.core.data.db.dao.RoutineDao
import com.ronin.ai.core.data.db.dao.RoutineHistoryDao
import com.ronin.ai.core.data.security.SecureVault
import com.ronin.ai.core.device.DeviceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ------------------------------------------------------------ Room
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoninDatabase =
        Room.databaseBuilder(context, RoninDatabase::class.java, "ronin.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMemoryDao(db: RoninDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideConversationDao(db: RoninDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideExperienceDao(db: RoninDatabase): ExperienceDao = db.experienceDao()

    @Provides
    fun provideRoutineDao(db: RoninDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideRoutineHistoryDao(db: RoninDatabase): RoutineHistoryDao = db.routineHistoryDao()

    @Provides
    fun provideNotificationEventDao(db: RoninDatabase): NotificationEventDao = db.notificationEventDao()

    // ------------------------------------------------------------ security
    @Provides
    @Singleton
    fun provideSecureVault(): SecureVault = SecureVault()

    // ------------------------------------------------------------ network
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(Constants.AI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Keep the pool small: RONIN talks to one provider at a time and
            // idle sockets cost memory on low-end devices.
            .connectionPool(ConnectionPool(2, 3, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)

        // Reject plaintext at the client level as well as in the manifest.
        builder.addInterceptor { chain ->
            val request = chain.request()
            if (!request.url.isHttps) {
                throw IOException("Refusing to send credentials over plaintext HTTP — use an https:// endpoint")
            }
            chain.proceed(request)
        }

        // HTTP logging is DEBUG-only and redacted. Gemini passes the API key in
        // the query string, so logging full URLs (the previous BASIC level, on
        // in release builds too) leaked live credentials to logcat.
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                Log.d("RoninHttp", message.redactSecrets())
            }.apply { level = HttpLoggingInterceptor.Level.BASIC }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    /** Strips API keys out of URLs/headers before anything reaches logcat. */
    private fun String.redactSecrets(): String = this
        .replace(Regex("""(?i)([?&]key=)[^&\s]+"""), "$1***")
        .replace(Regex("""(?i)(Authorization:\s*Bearer\s+)\S+"""), "$1***")
        .replace(Regex("""(?i)(xi-api-key:\s*)\S+"""), "$1***")
        .replace(Regex("""(?i)(Ocp-Apim-Subscription-Key:\s*)\S+"""), "$1***")

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.ronin.local/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi = retrofit.create(AiApi::class.java)

    // ------------------------------------------------------------ device
    @Provides
    @Singleton
    fun provideDeviceManager(@ApplicationContext context: Context): DeviceManager =
        com.ronin.ai.core.device.AndroidDeviceManager(context)
}
