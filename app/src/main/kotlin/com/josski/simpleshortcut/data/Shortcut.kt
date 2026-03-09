package com.josski.simpleshortcut.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortcuts")
data class Shortcut(
    @PrimaryKey
    val id: String,
    val label: String,
    val packageName: String,
    val deeplink: String,
    val emoji: String,
    val rank: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "")
    val category: String = "",
    @ColumnInfo(defaultValue = "0")
    val isPinned: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val tapCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val lastUsedAt: Long = 0
)