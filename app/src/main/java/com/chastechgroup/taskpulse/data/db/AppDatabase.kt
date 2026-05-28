package com.chastechgroup.taskpulse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chastechgroup.taskpulse.data.db.dao.AppInfoDao
import com.chastechgroup.taskpulse.data.db.dao.RuleDao
import com.chastechgroup.taskpulse.data.db.dao.SessionDao
import com.chastechgroup.taskpulse.data.entities.*

@Database(
    entities = [
        RuleEntity::class,
        AppInfoEntity::class,
        SessionEntity::class,
        BlockedAppEntity::class,
        NotificationFilterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RuleConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskpulse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
