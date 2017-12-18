package basketcasey.com.androidsecurenote.database;

import android.database.sqlite.SQLiteDatabase;

public class GroupsTable {
	public static final String TABLE_GROUPS = "groups";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	
	private static final String DATABASE_CREATE = "create table "
		      + TABLE_GROUPS + "(" + COLUMN_ID
		      + " integer primary key autoincrement, " 
		      + COLUMN_NAME + " text not null);"; 
	
	public static void onCreate(SQLiteDatabase database) {
		    database.execSQL(DATABASE_CREATE);    
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		    database.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
		    onCreate(database);
	}
}