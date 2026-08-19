package com.ronin.ai.core.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
            .fallbackToDestructiveMigration(false)
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
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(Constants.AI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

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
