package com.guidovezzoni.venice.domain.model

data class Trip(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val stopCount: Int = 0,
)
