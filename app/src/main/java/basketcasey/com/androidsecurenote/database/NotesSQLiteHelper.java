package basketcasey.com.androidsecurenote.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesSQLiteHelper  extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "notes.db";
	private static final int DATABASE_VERSION = 1;
	
	public NotesSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		NotesTable.onCreate(database);
		GroupsTable.onCreate(database);
		ConfigurationTable.onCreate(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		NotesTable.onUpgrade(database, oldVersion, newVersion);
		GroupsTable.onUpgrade(database, oldVersion, newVersion);
		ConfigurationTable.onUpgrade(database, oldVersion, newVersion);
	}
	
}
