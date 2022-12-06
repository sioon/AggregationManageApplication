package net.mbiz.aggregationmanageapplication.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(
    context: Context?,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int
) : SQLiteOpenHelper(context, name, factory, version) {
    override fun onCreate(database: SQLiteDatabase?) {
        var create = "CREATE TABLE if not exists barcode(" +
                "id integer primary key autoincrement," +
                "barcodeNum);"
        database?.execSQL(create)
    }

    override fun onUpgrade(database: SQLiteDatabase?, oldVer: Int, newVer: Int) {
        TODO("Not yet implemented")
    }
}