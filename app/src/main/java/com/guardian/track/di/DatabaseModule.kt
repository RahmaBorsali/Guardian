package com.guardian.track.di

import android.content.Context
import androidx.room.Room
import com.guardian.track.data.local.GuardianDatabase
import com.guardian.track.data.local.dao.EmergencyContactDao
import com.guardian.track.data.local.dao.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room Database and DAO instances as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GuardianDatabase {
        return Room.databaseBuilder(
            context,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideIncidentDao(database: GuardianDatabase): IncidentDao {
        return database.incidentDao()
    }

    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: GuardianDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }
}
