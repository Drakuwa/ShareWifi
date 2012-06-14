package com.app.sharewifi;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBAdapter 
{
    public static final String AP_ID = "_id";
    public static final String AP_TIMESTAMP = "timestamp";
    public static final String AP_BSSID = "bssid";
    public static final String AP_NAME = "name";
    public static final String AP_PASSWORD = "password";
    public static final String AP_WEP = "wep";
    public static final String AP_LOCATION = "location";
    public static final String AP_COUNTRY = "country";
    
    private static final String DATABASE_NAME = "apdb";    
    private static final String DATABASE_TABLE = "aps";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
        "create table if not exists aps (" +
        "_id integer primary key autoincrement, " +
        "timestamp text not null," +
		"bssid text not null," +
		"name text not null," +
		"wep text not null," +
		"location text not null," +
		"country text not null," +
		"password text not null" +
        ");";
        
    private final Context context; 
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            db.execSQL("DROP TABLE IF EXISTS aps");
            onCreate(db);
        }
    }    
    
    //---opens the database---
    public DBAdapter open() throws SQLException 
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }
    
    //---insert an AP into the database---
    public long insertAP(ArrayList<String> ap) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(AP_TIMESTAMP, ap.get(0));
        initialValues.put(AP_BSSID, ap.get(1));
        initialValues.put(AP_NAME, ap.get(2));
        initialValues.put(AP_WEP, ap.get(3));
        initialValues.put(AP_LOCATION, ap.get(4));
        initialValues.put(AP_COUNTRY, ap.get(5));
        initialValues.put(AP_PASSWORD, ap.get(6));
        return db.insert(DATABASE_TABLE, null, initialValues);
    }
    
    //---retrieve a particular AP---
    public Cursor getAP(String id) throws SQLException {
		Cursor mCursor = db.query(true, DATABASE_TABLE, new String[] {
				AP_ID, AP_TIMESTAMP, AP_BSSID, AP_NAME, AP_WEP,
				AP_LOCATION, AP_COUNTRY, AP_PASSWORD }, AP_ID + " LIKE "
				+ "'" + id + "'", null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
    
    //---deletes a particular AP---
    public boolean deleteAP(long rowId) 
    {
        return db.delete(DATABASE_TABLE, AP_ID + 
        		"=" + rowId, null) > 0;
    }
    //---retrieves all the APs---
    public Cursor getAllAPs() 
    {
        return db.query(DATABASE_TABLE, new String[] {
        		AP_ID,
        		AP_TIMESTAMP,
        		AP_BSSID,
        		AP_NAME,
        		AP_WEP,
        		AP_LOCATION,
        		AP_COUNTRY,
        		AP_PASSWORD}, 
                null, 
                null, 
                null, 
                null, 
                null);
    }
    //---updates an AP---
    public boolean updateAP(long rowId, ArrayList<String> ap) 
    {
        ContentValues args = new ContentValues();
        args.put(AP_TIMESTAMP, ap.get(0));
        args.put(AP_BSSID, ap.get(1));
        args.put(AP_NAME, ap.get(2));
        args.put(AP_WEP, ap.get(3));
        args.put(AP_LOCATION, ap.get(4));
        args.put(AP_COUNTRY, ap.get(5));
        args.put(AP_PASSWORD, ap.get(6));
        return db.update(DATABASE_TABLE, args, 
                         AP_ID + "=" + rowId, null) > 0;
    }
}
