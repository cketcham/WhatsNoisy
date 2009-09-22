package edu.ucla.cens.whatsnoisy.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class LocationDatabase {
		
	public static final String KEY_LOCATION_LATITUDE = "location_latitude";
	public static final String KEY_LOCATION_LONGITUDE = "location_longitude";
	public static final String KEY_LOCATION_TIME = "location_time";
	public static final String KEY_LOCATION_PROVIDER = "location_provider";
	public static final String KEY_LOCATION_ACCURACY = "location_accuracy";
	public static final String KEY_LOCATION_SPEED = "location_speed";
	public static final String KEY_LOCATION_ALTITUDE = "location_altitude";
	public static final String KEY_LOCATION_BEARING = "location_bearing";
	public static final String KEY_LOCATION_ROWID = "_id";
	private static boolean databaseOpen = false;
	private static Object dbLock = new Object();
	public static final String TAG = "locationDB";
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;
	
	private Context mCtx = null;
	
	private static final String DATABASE_NAME = "location_db";
	private static final String DATABASE_TABLE = "location_table";
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_CREATE = "create table location_table (_id integer primary key autoincrement, "
		+ "location_latitude text not null,"
		+ "location_longitude text not null,"
		+ "location_time text not null,"
		+ "location_provider text not null,"
		+ "location_accuracy text not null,"
		+ "location_speed text not null,"
		+ "location_bearing text not null,"
		+ "location_altitude text not null"
		+ ");";
	
    public class LocationRow extends Object {
    	public long key;
    	public Location location = new Location("");
    }
	
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context ctx)
		{
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			db.execSQL("DROP TABLE EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}		
	}
	
	public LocationDatabase(Context ctx)
	{
		mCtx = ctx;
	}
	
	public LocationDatabase open() throws SQLException{

		synchronized(dbLock)
		{
			while (databaseOpen)
			{
				try
				{
					dbLock.wait();
				}
				catch (InterruptedException e){}

			}
			databaseOpen = true;
			dbHelper = new DatabaseHelper(mCtx);
			db = dbHelper.getWritableDatabase();

			return this;
		}		
	}
	
	public void close()
	{
		synchronized(dbLock)
		{
			dbHelper.close();
			databaseOpen = false;
			dbLock.notify();
		}
	}
	
	public long createPoint(String latValue, String lonValue, String timeValue, String providerValue, String accuracyValue, String speedValue, String bearingValue, String altitudeValue)
	{		
		ContentValues vals = new ContentValues();
		vals.put(KEY_LOCATION_LONGITUDE, lonValue);
		vals.put(KEY_LOCATION_LATITUDE, latValue);
		vals.put(KEY_LOCATION_TIME, timeValue);
		vals.put(KEY_LOCATION_PROVIDER, providerValue);
		vals.put(KEY_LOCATION_ACCURACY, accuracyValue);
		vals.put(KEY_LOCATION_SPEED, speedValue);
		vals.put(KEY_LOCATION_BEARING, bearingValue);
		vals.put(KEY_LOCATION_ALTITUDE, altitudeValue);
		
		long rowid = db.insert(DATABASE_TABLE, null, vals);
		return rowid;
	}
	
	public long createPoint(Location l)
	{
		return createPoint(Double.toString(l.getLatitude()),Double.toString(l.getLongitude()),Long.toString(l.getTime()),l.getProvider(),Float.toString(l.getAccuracy()),Float.toString(l.getSpeed()),Float.toString(l.getBearing()),Double.toString(l.getAltitude()));
	}
	
	public boolean deletePoint(long rowId)
	{
		int count = 0;
		count = db.delete(DATABASE_TABLE, KEY_LOCATION_ROWID+"="+rowId, null);
		
		if(count > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean hasSamples() {
		Cursor c = db.query(DATABASE_TABLE, null, null, null, null, null, null, "1");
		int count = c.getCount();
		c.close();

		if(count > 0)
			return true;
		else
			return false;
	}
	
	public ArrayList <LocationRow>  fetchAllPoints() {
		ArrayList<LocationRow> ret = new ArrayList<LocationRow>();

			Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_LOCATION_ROWID, KEY_LOCATION_LONGITUDE, KEY_LOCATION_LATITUDE, KEY_LOCATION_TIME, KEY_LOCATION_PROVIDER, KEY_LOCATION_ACCURACY, KEY_LOCATION_SPEED, KEY_LOCATION_BEARING, KEY_LOCATION_ALTITUDE}, null, null, null, null, null);
			int numRows = c.getCount();
			
			c.moveToFirst();
			
			for (int i =0; i < numRows; ++i)
			{
				LocationRow lr = new LocationRow();
				
				lr.key = c.getLong(0);
				lr.location.setLatitude(new Double(c.getString(2)));
				lr.location.setLongitude(new Double(c.getString(1)));
				lr.location.setTime(new Long(c.getString(3)));
				lr.location.setProvider(c.getString(4));
				lr.location.setAccuracy(new Float(c.getString(5)));
				lr.location.setSpeed(new Float(c.getString(6)));
				lr.location.setBearing(new Float(c.getString(7)));
				lr.location.setAltitude(new Double(c.getString(8)));
				
				ret.add(lr);
				
				c.moveToNext();
				
			}
			c.close();			

		return ret;
	}
	
	public LocationRow fetchPoint(long rowId) throws SQLException
	{
		Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_LOCATION_ROWID, KEY_LOCATION_LONGITUDE, KEY_LOCATION_LATITUDE, KEY_LOCATION_TIME, KEY_LOCATION_PROVIDER, KEY_LOCATION_ACCURACY, KEY_LOCATION_SPEED, KEY_LOCATION_BEARING, KEY_LOCATION_ALTITUDE}, KEY_LOCATION_ROWID+"="+rowId, null, null, null, null);
		LocationRow ret = new LocationRow();

		if (c != null) {
			c.moveToFirst();
						
			ret.key = c.getLong(0);
			ret.location.setLatitude(new Double(c.getString(2)));
			ret.location.setLongitude(new Double(c.getString(1)));
			ret.location.setTime(new Long(c.getString(3)));
			ret.location.setProvider(c.getString(4));
			ret.location.setAccuracy(new Float(c.getString(5)));
			ret.location.setSpeed(new Float(c.getString(6)));
			ret.location.setBearing(new Float(c.getString(7)));
			ret.location.setAltitude(new Double(c.getString(8)));
		}
		else
		{
			ret.key = -1;
			ret.location = null;
		}
		c.close();
		return ret;
	}
	
	public boolean updatePoint(long rowId, String lonValue, String latValue, String timeValue, String providerValue, String accuracyValue, String speedValue, String bearingValue, String altitudeValue) {
		ContentValues vals = new ContentValues();
		vals.put(KEY_LOCATION_LONGITUDE, lonValue);
		vals.put(KEY_LOCATION_LATITUDE, latValue);
		vals.put(KEY_LOCATION_TIME, timeValue);
		vals.put(KEY_LOCATION_PROVIDER, providerValue);
		vals.put(KEY_LOCATION_ACCURACY, accuracyValue);
		vals.put(KEY_LOCATION_SPEED, speedValue);
		vals.put(KEY_LOCATION_BEARING, bearingValue);
		vals.put(KEY_LOCATION_ALTITUDE, altitudeValue);
		
		return db.update(DATABASE_TABLE, vals,KEY_LOCATION_ROWID+"="+rowId, null) > 0;
	}
}