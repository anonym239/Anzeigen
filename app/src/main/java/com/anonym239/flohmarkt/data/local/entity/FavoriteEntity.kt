package com.anonym239.flohmarkt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dateTime: String,
    val location: String,
    val address: String,
    val category: String,
    val url: String,
    val imageUrl: String?
)
