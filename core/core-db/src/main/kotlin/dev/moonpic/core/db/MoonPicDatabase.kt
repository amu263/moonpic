package dev.moonpic.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LutEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MoonPicDatabase : RoomDatabase() {
    abstract fun lutDao(): LutDao

    companion object {
        const val NAME = "moonpic.db"
    }
}
