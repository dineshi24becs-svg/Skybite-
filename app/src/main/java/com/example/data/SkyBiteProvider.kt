package com.example.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.ContentUris
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase

class SkyBiteProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.skybite.provider"
        
        const val FOOD_ITEMS = 1
        const val ORDERS = 2
        const val USERS = 3

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "food_items", FOOD_ITEMS)
            addURI(AUTHORITY, "order_history", ORDERS)
            addURI(AUTHORITY, "users", USERS)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val db = AppDatabase.getDatabase(context).openHelper.readableDatabase
        val table = when (uriMatcher.match(uri)) {
            FOOD_ITEMS -> "food_items"
            ORDERS -> "order_history"
            USERS -> "users"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        // Build simple query
        val sql = StringBuilder("SELECT * FROM $table")
        if (!selection.isNullOrBlank()) {
            sql.append(" WHERE $selection")
        }
        if (!sortOrder.isNullOrBlank()) {
            sql.append(" ORDER BY $sortOrder")
        }

        val cursor = db.query(sql.toString(), selectionArgs ?: emptyArray())
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val context = context ?: return null
        val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
        val table = when (uriMatcher.match(uri)) {
            FOOD_ITEMS -> "food_items"
            ORDERS -> "order_history"
            USERS -> "users"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val nonNullValues = values ?: ContentValues()
        val id = db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, nonNullValues)
        if (id > 0) {
            val newUri = ContentUris.withAppendedId(uri, id)
            context.contentResolver.notifyChange(uri, null)
            return newUri
        }
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val context = context ?: return 0
        val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
        val table = when (uriMatcher.match(uri)) {
            FOOD_ITEMS -> "food_items"
            ORDERS -> "order_history"
            USERS -> "users"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val nonNullValues = values ?: ContentValues()
        val rows = db.update(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, nonNullValues, selection, selectionArgs)
        if (rows > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return rows
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val context = context ?: return 0
        val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
        val table = when (uriMatcher.match(uri)) {
            FOOD_ITEMS -> "food_items"
            ORDERS -> "order_history"
            USERS -> "users"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val rows = db.delete(table, selection, selectionArgs)
        if (rows > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return rows
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            FOOD_ITEMS -> "vnd.android.cursor.dir/$AUTHORITY.food_items"
            ORDERS -> "vnd.android.cursor.dir/$AUTHORITY.order_history"
            USERS -> "vnd.android.cursor.dir/$AUTHORITY.users"
            else -> null
        }
    }
}
