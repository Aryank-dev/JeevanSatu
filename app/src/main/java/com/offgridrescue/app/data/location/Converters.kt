package com.offgridrescue.app.data.location

import androidx.room.TypeConverter
import com.offgridrescue.app.domain.CheckInStatus

class Converters {
    @TypeConverter
    fun fromCheckInStatus(value: CheckInStatus): String {
        return value.name
    }

    @TypeConverter
    fun toCheckInStatus(value: String): CheckInStatus {
        return CheckInStatus.valueOf(value)
    }
}
