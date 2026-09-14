package com.offgridrescue.app.data.location

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LocationRecordEntity::class, CheckInRecordEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocationTrackingDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun checkInDao(): CheckInDao

    companion object {
        @Volatile
        private var INSTANCE: LocationTrackingDatabase? = null

        fun getDatabase(context: Context): LocationTrackingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationTrackingDatabase::class.java,
                    "location_tracking_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
