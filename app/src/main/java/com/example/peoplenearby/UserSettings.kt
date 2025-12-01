package com.example.peoplenearby

import android.content.Context
import java.util.UUID

class UserSettings (private val context: Context) {

    fun getOrCreateUserId(): String {
        val prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val saved = prefs.getString("USER_ID", null)

        if (saved != null) return saved

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("USER_ID", newId).apply()
        return newId
    }
}