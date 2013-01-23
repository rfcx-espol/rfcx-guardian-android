package org.rfcx.src_database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.rfcx.src_util.DateTimeUtils;

public class ArduinoDb {

	public ArduinoDb(Context context) {
		this.dbHumidity = new DbHumidity(context);
		this.dbTemperature = new DbTemperature(context);
		this.dbCharge = new DbCharge(context);
	}
	
	private static final String TAG = ArduinoDb.class.getSimpleName();
	static final int VERSION = 1;
	static final String DATABASE = "arduino";
	static final String C_CREATED_AT = "created_at";
	static final String C_VALUE = "value";
	static final String CREATE_CLMNS = "(" + C_CREATED_AT + " DATETIME, " + C_VALUE + " INT " + ")";
		
	// for saving humidity values
	public class DbHumidity {
		private String TABLE = "humidity";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				String sqlCreate = "CREATE TABLE " + TABLE + CREATE_CLMNS;
				db.execSQL(sqlCreate);
				Log.d(TAG, "onCreate() " + sqlCreate);
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE);
				Log.d(TAG, "onUpgrade()");
				onCreate(db);
			}
		}
		final DbHelper dbHelper;
		public DbHumidity(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getCurrDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
				Log.d(TAG, "insert: "+values);
			} finally {
				db.close();
			}
		}
	}
	public final DbHumidity dbHumidity;
	
	// for saving temperature values
	public class DbTemperature {
		private String TABLE = "temperature";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				String sqlCreate = "CREATE TABLE " + TABLE + CREATE_CLMNS;
				db.execSQL(sqlCreate);
				Log.d(TAG, "onCreate() " + sqlCreate);
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE);
				Log.d(TAG, "onUpgrade()");
				onCreate(db);
			}
		}
		final DbHelper dbHelper;
		public DbTemperature(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getCurrDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
				Log.d(TAG, "insert: "+values);
			} finally {
				db.close();
			}
		}
	}
	public final DbTemperature dbTemperature;
	
	// for saving battery charging state values
	public class DbCharge {
		private String TABLE = "charge";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				String sqlCreate = "CREATE TABLE " + TABLE + CREATE_CLMNS;
				db.execSQL(sqlCreate);
				Log.d(TAG, "onCreate() " + sqlCreate);
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE);
				Log.d(TAG, "onUpgrade()");
				onCreate(db);
			}
		}
		final DbHelper dbHelper;
		public DbCharge(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getCurrDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
				Log.d(TAG, "insert: "+values);
			} finally {
				db.close();
			}
		}
	}
	public final DbCharge dbCharge;
	
}
