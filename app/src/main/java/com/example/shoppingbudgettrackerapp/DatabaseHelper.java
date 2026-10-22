package com.example.shoppingbudgettrackerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ShoppingBudget.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "price REAL NOT NULL," +
                        "quantity INTEGER NOT NULL)"
        );

        db.execSQL(
                "CREATE TABLE settings (" +
                        "id INTEGER PRIMARY KEY," +
                        "budget REAL NOT NULL)"
        );

        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("budget", 0);

        db.insert("settings", null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion,
                          int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS items");
        db.execSQL("DROP TABLE IF EXISTS settings");

        onCreate(db);
    }

    public boolean addItem(String name, double price, int quantity) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("price", price);
        values.put("quantity", quantity);

        long result = db.insert("items", null, values);

        return result != -1;
    }

    public Cursor getAllItems() {

        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery(
                "SELECT id, name, price, quantity FROM items ORDER BY id DESC",
                null
        );
    }

    public boolean deleteItem(int id) {

        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                "items",
                "id = ?",
                new String[]{String.valueOf(id)}
        ) > 0;
    }

    public void setBudget(double budget) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("budget", budget);

        db.update(
                "settings",
                values,
                "id = ?",
                new String[]{"1"}
        );
    }

    public double getBudget() {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT budget FROM settings WHERE id = 1",
                null
        );

        double budget = 0;

        if (cursor.moveToFirst()) {
            budget = cursor.getDouble(0);
        }

        cursor.close();

        return budget;
    }

    public boolean clearAllItems() {

        SQLiteDatabase db = getWritableDatabase();

        return db.delete("items", null, null) >= 0;
    }
}
