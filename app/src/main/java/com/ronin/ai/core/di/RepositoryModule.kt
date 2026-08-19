package com.ronin.ai.core.di

import com.ronin.ai.core.data.repository.ConversationRepositoryImpl
import com.ronin.ai.core.data.repository.ExperienceRepositoryImpl
import com.ronin.ai.core.data.repository.MemoryRepositoryImpl
import com.ronin.ai.core.data.repository.NotificationRepositoryImpl
import com.ronin.ai.core.data.repository.RoutineRepositoryImpl
import com.ronin.ai.core.data.repository.SettingsRepositoryImpl
import com.ronin.ai.core.domain.repository.ConversationRepository
import com.ronin.ai.core.domain.repository.ExperienceRepository
import com.ronin.ai.core.domain.repository.MemoryRepository
import com.ronin.ai.core.domain.repository.NotificationRepository
import com.ronin.ai.core.domain.repository.RoutineRepository
import com.ronin.ai.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their Room/DataStore-backed implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindExperienceRepository(impl: ExperienceRepositoryImpl): ExperienceRepository

    @Binds
    @Singleton
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
