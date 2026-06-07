package dev.moonpic.core.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoonPicDatabase =
        Room.databaseBuilder(context, MoonPicDatabase::class.java, MoonPicDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLutDao(db: MoonPicDatabase): LutDao = db.lutDao()
}
