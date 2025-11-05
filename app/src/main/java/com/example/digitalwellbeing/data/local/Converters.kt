package com.example.digitalwellbeing.data.local

import androidx.room.TypeConverter
import com.example.digitalwellbeing.data.model.ChallengeType

class Converters {
    @TypeConverter
    fun fromChallengeType(value: ChallengeType): String {
        return value.name
    }

    @TypeConverter
    fun toChallengeType(value: String): ChallengeType {
        return ChallengeType.valueOf(value)
    }
}
