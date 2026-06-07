package dev.moonpic.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "luts")
data class LutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "size") val size: Int,
    @ColumnInfo(name = "domain_min") val domainMin: Float,
    @ColumnInfo(name = "domain_max") val domainMax: Float,
    @ColumnInfo(name = "raw_path") val rawPath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)
