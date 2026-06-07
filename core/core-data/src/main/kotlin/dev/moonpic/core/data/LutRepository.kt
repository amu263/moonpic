package dev.moonpic.core.data

import dev.moonpic.core.db.LutDao
import dev.moonpic.core.db.LutEntity
import dev.moonpic.core.domain.LutSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LutRepository @Inject constructor(
    private val lutDao: LutDao,
) {
    fun observeAll(): Flow<List<LutSummary>> =
        lutDao.observeAll().map { rows -> rows.map { it.toSummary() } }

    suspend fun import(
        name: String,
        title: String,
        size: Int,
        domainMin: Float,
        domainMax: Float,
        rawPath: String,
    ): Long = lutDao.upsert(
        LutEntity(
            name = name,
            title = title,
            size = size,
            domainMin = domainMin,
            domainMax = domainMax,
            rawPath = rawPath,
        ),
    )

    suspend fun delete(id: Long) {
        lutDao.findById(id)?.let { lutDao.delete(it) }
    }
}

private fun LutEntity.toSummary() = LutSummary(
    id = id,
    name = name,
    title = title,
    size = size,
    domainMin = domainMin,
    domainMax = domainMax,
    createdAt = createdAt,
)
