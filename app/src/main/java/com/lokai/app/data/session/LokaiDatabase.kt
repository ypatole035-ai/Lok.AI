package com.lokai.app.data.session

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DownloadedModelEntity::class,
        ChatSessionEntity::class,
        AgentProfileEntity::class,
        FileChunkEntity::class,
        AgentSessionEntity::class
    ],
    version  = 5,
    exportSchema = false
)
@TypeConverters(ChatConverters::class, AgentConverters::class)
abstract class LokaiDatabase : RoomDatabase() {

    abstract fun downloadedModelDao(): DownloadedModelDao
    abstract fun sessionDao(): SessionDao
    abstract fun agentDao(): AgentDao
    abstract fun chunkDao(): ChunkDao
    abstract fun agentSessionDao(): AgentSessionDao

    companion object {
        @Volatile
        private var INSTANCE: LokaiDatabase? = null

        fun getInstance(context: Context): LokaiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LokaiDatabase::class.java,
                    "lokai_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
