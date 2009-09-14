package edu.ucla.cens.whatsnoisy.data;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import edu.ucla.cens.whatsnoisy.whatsnoisy;
import edu.ucla.cens.whatsnoisy.services.SampleUpload;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class SampleDatabase {
	public static final String KEY_ID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_PATH = "path";
	public static final String KEY_LATITUDE = "lat";
	public static final String KEY_LONGITUDE = "lon";
	public static final String KEY_TYPE = "type";

	private static final String DATABASE_NAME = "sample_db";
	
	//sqlite3 /data/data/edu.ucla.cens.whatsnoisy/databases/samples.db

	private static final String DATABASE_TABLE = "samples";

	private static final int DATABASE_VERSION = 1;

	private static boolean databaseOpen = false;
	private static Object dbLock = new Object();
	private final DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	private Context context = null;

	private static final String DATABASE_CREATE = "create table " +
		DATABASE_TABLE 
		+ " (" 
		+ KEY_ID + " integer primary key autoincrement, "
		+ KEY_TITLE + " text not null,"
		+ KEY_PATH + " text not null,"
		+ KEY_LONGITUDE + " double not null,"
		+ KEY_LATITUDE + " double not null,"
		+ KEY_TYPE + " text not null"
		+ ");";

	public static class SampleRow {
		public Long key;
		public String title;
		public String path;
		public Location location = new Location(LocationManager.GPS_PROVIDER);
		public String type;
		public Date datetime;
		
		public String getLocation() {
			return Double.toString(this.location.getLatitude()) + "," + Double.toString(this.location.getLongitude());
		}
		
		public String getDatetime() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");						
			return dateFormat.format(datetime);
		}
		
		public void setDatetime(String string) throws ParseException {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");						
			datetime = dateFormat.parse(string);
		}
	}

	public SampleDatabase(Context context) {
		this.context = context;
		dbHelper = new DatabaseHelper(this.context);
	}

	public SampleDatabase openRead() throws SQLException {
		db = dbHelper.getReadableDatabase();

		return this;
	}

	public SampleDatabase openWrite() throws SQLException {

		synchronized (dbLock) {
			while (databaseOpen) {
				try {
					dbLock.wait();
				} catch (InterruptedException e) {
				}

			}

			databaseOpen = true;
			db = dbHelper.getWritableDatabase();

			return this;
		}
	}

	public void close() {
		if (db.isReadOnly()) {
			db.close();
		} else {
			synchronized (dbLock) {
				dbHelper.close();
				databaseOpen = false;

				dbLock.notify();
			}
		}
	}

	public SampleRow getSample(String key) {
		Cursor c = db.query(DATABASE_TABLE, new String[] { KEY_ID,
				KEY_TITLE, KEY_PATH, KEY_TYPE, KEY_LATITUDE,
				KEY_LONGITUDE }, KEY_ID + "=?", new String[] { String
				.valueOf(key) }, null, null, null);
		SampleRow ret = new SampleRow();

		if (c.moveToFirst()) {
			ret.key = c.getLong(0);
			ret.title = c.getString(1);
			ret.path = c.getString(2);
			ret.type = c.getString(3);
			ret.location.setLatitude(c.getDouble(4));
			ret.location.setLongitude(c.getDouble(5));
		}

		c.close();

		return ret;
	}

	public long insertSample(SampleRow row) {
		ContentValues vals = new ContentValues();
		vals.put(SampleDatabase.KEY_TITLE, row.title);
		vals.put(SampleDatabase.KEY_PATH, row.path);
		vals.put(SampleDatabase.KEY_TYPE, row.type);
		vals.put(SampleDatabase.KEY_LATITUDE, row.location.getLatitude());
		vals.put(SampleDatabase.KEY_LONGITUDE, row.location.getLongitude());

		long rowid = db.insert(DATABASE_TABLE, null, vals);
		
		return rowid;
	}

	public boolean clearSamples(String type) {
		// Remove thumbnails
		Cursor c = db.query(DATABASE_TABLE, new String[] { KEY_ID,
				KEY_PATH }, KEY_TYPE + "=?", new String[] { String
				.valueOf(type) }, null, null, null);

		while (c.moveToNext()) {
			String path = c.getString(c
					.getColumnIndex(SampleDatabase.KEY_PATH));
			new File(path).delete();
		}

		c.close();

		int count = db.delete(DATABASE_TABLE, KEY_TYPE + "=?",
				new String[] { String.valueOf(type) });

		if (count > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean deleteSample(long rowId)
	{
		int count = 0;
		count = db.delete(DATABASE_TABLE, KEY_ID+"="+rowId, null);
		
		if(count > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}


	public boolean clearAllSamples() {
		int count = db.delete(DATABASE_TABLE, null, null);

		if (count > 0) {
			return true;
		} else {
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

	public Cursor getSamples(String type) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_TITLE,
				KEY_PATH, KEY_LATITUDE, KEY_LONGITUDE },
				KEY_TYPE + "=?", new String[] { String.valueOf(type) }, null,
				null, null);
	}
	
	public ArrayList <SampleRow>  fetchAllSamples() {
		ArrayList<SampleRow> ret = new ArrayList<SampleRow>();

			Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_ID, KEY_TITLE, KEY_PATH, KEY_LATITUDE, KEY_LONGITUDE, KEY_TYPE}, null, null, null, null, null);
			int numRows = c.getCount();
			
			c.moveToFirst();
			
			for (int i =0; i < numRows; ++i)
			{
				SampleRow sr = new SampleRow();
				
				sr.key = c.getLong(0);
				sr.title = c.getString(1);
				sr.path = c.getString(2);
				sr.location.setLatitude(c.getDouble(3));
				sr.location.setLongitude(c.getDouble(4));
				sr.type = c.getString(5);
				
				ret.add(sr);
				
				c.moveToNext();
				
			}
			c.close();			

		return ret;
	}

	public Cursor getAllSamples() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_TITLE,
				KEY_PATH, KEY_LATITUDE, KEY_LONGITUDE },
				null, null, null, null, null);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// db.execSQL("DROP TABLE EXISTS " + DATABASE_TABLE);
			// onCreate(db);
		}
	}
}
