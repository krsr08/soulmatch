package com.soulmatch.app.di
import android.content.Context
import com.soulmatch.app.data.local.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideUserPreferences(@ApplicationContext ctx: Context): UserPreferences = UserPreferences(ctx)
}
