package basketcasey.com.androidsecurenote.database;

import android.database.sqlite.SQLiteDatabase;

public class NotesTable {
	public static final String TABLE_NOTES = "notes";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_GROUP_ID = "group_id";
	public static final String COLUMN_PRIORITY = "priority";
	public static final String COLUMN_TITLE = "title";
	public static final String COLUMN_DESCRIPTION = "description";
		
	private static final String DATABASE_CREATE = "create table "
		      + TABLE_NOTES + "(" + COLUMN_ID
		      + " integer primary key autoincrement, " 
		      + COLUMN_GROUP_ID + " long not null, "
		      + COLUMN_PRIORITY + " int not null, " 
		      + COLUMN_TITLE + " text not null, "
		      + COLUMN_DESCRIPTION + " text);";
	
	public static void onCreate(SQLiteDatabase database) {
		    database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		    database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
		    onCreate(database);
	}
}
