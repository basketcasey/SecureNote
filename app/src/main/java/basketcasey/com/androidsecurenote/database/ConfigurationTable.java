package basketcasey.com.androidsecurenote.database;

import android.database.sqlite.SQLiteDatabase;

public class ConfigurationTable {
	public static final String TABLE_CONFIGURATION = "configuration";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_SALT = "salt";
	public static final String COLUMN_TESTVALUE = "testvalue"; // used to store word "VALID" encrypted with generated key to validate password at logon
	
	private static final String DATABASE_CREATE = "create table "
		      + TABLE_CONFIGURATION + "(" + COLUMN_ID
		      + " integer primary key autoincrement, "
		      + COLUMN_TESTVALUE + " text not null, "
		      + COLUMN_SALT + " text not null);"; 
	
	public static void onCreate(SQLiteDatabase database) {
		    database.execSQL(DATABASE_CREATE);    
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		    database.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIGURATION);
		    onCreate(database);
	}
}
