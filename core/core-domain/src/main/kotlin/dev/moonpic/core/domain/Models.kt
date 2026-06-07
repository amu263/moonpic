package dev.moonpic.core.domain

data class LutSummary(
    val id: Long,
    val name: String,
    val title: String,
    val size: Int,
    val domainMin: Float,
    val domainMax: Float,
    val createdAt: Long,
)
