package com.focus.digitalwellbeing.data.local

import androidx.room.TypeConverter
import com.focus.digitalwellbeing.data.model.ChallengeType
import com.focus.digitalwellbeing.data.model.FocusType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromChallengeType(value: ChallengeType): String {
        return value.name
    }

    @TypeConverter
    fun toChallengeType(value: String): ChallengeType {
        return ChallengeType.valueOf(value)
    }

    @TypeConverter
    fun fromFocusType(value: FocusType): String {
        return value.name
    }

    @TypeConverter
    fun toFocusType(value: String): FocusType {
        return FocusType.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
}

