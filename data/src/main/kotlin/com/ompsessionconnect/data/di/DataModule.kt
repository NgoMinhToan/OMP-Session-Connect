package com.ompsessionconnect.data.di

import android.content.Context
import com.ompsessionconnect.data.crypto.LinkCipher
import com.ompsessionconnect.data.repository.SessionRepositoryImpl
import com.ompsessionconnect.data.repository.SettingsRepositoryImpl
import com.ompsessionconnect.data.runtime.SessionRuntimeManager
import com.ompsessionconnect.data.store.SessionStore
import com.ompsessionconnect.data.store.SettingsStore
import com.ompsessionconnect.data.update.UpdateRepositoryImpl
import com.ompsessionconnect.domain.repository.SessionRepository
import com.ompsessionconnect.domain.repository.SettingsRepository
import com.ompsessionconnect.domain.repository.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .pingInterval(20, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  @Provides
  @Singleton
  fun provideLinkCipher(): LinkCipher = LinkCipher()

  @Provides
  @Singleton
  fun provideSessionStore(@ApplicationContext context: Context): SessionStore = SessionStore(context)

  @Provides
  @Singleton
  fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore = SettingsStore(context)

  @Provides
  @Singleton
  fun provideRuntimeManager(
    store: SessionStore,
    linkCipher: LinkCipher,
    client: OkHttpClient,
  ): SessionRuntimeManager = SessionRuntimeManager(store, linkCipher, client)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
  @Binds
  @Singleton
  abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

  @Binds
  @Singleton
  abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

  @Binds
  @Singleton
  abstract fun bindUpdateRepository(impl: UpdateRepositoryImpl): UpdateRepository
}
