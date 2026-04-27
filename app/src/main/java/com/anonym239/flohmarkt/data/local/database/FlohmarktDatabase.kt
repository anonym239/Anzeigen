package com.anonym239.flohmarkt.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anonym239.flohmarkt.data.local.dao.FavoriteDao
import com.anonym239.flohmarkt.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FlohmarktDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "flohmarkt_db"
    }
}
