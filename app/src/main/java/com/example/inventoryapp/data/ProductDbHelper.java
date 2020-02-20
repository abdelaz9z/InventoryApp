package com.example.inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.inventoryapp.data.ProductContract.ProductEntry;

public class ProductDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 2;

    private static final String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE IF NOT EXISTS " + ProductEntry.TABLE_NAME + " ("
            + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
            + ProductEntry.COLUMN_MODEL_NO + " TEXT, "
            + ProductEntry.COLUMN_PRICE + " INTEGER NOT NULL, "
            + ProductEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
            + ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT NOT NULL, "
            + ProductEntry.COLUMN_PRODUCT_GRADE + " INTEGER NOT NULL DEFAULT 0,"
            + ProductEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL, "
            + ProductEntry.COLUMN_SUPPLIER_PHONE + " INTEGER NOT NULL, "
            + ProductEntry.COLUMN_SUPPLIER_EMAIL + " TEXT);";

    private static final String SQL_DROP_INVENTORY_TABLE = "DROP TABLE IF EXISTS " + ProductEntry.TABLE_NAME;

    ProductDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_INVENTORY_TABLE);
        onCreate(db);
    }
}