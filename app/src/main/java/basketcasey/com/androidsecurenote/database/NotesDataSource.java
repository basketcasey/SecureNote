package basketcasey.com.androidsecurenote.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import basketcasey.com.androidsecurenote.cryptography.SymmetricKeyLib;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class NotesDataSource {
	// Database DAO class managing the database connection and data queries (CRUD)
	private SQLiteDatabase database;
	private NotesSQLiteHelper dbHelper;
	
	private String[] allNoteColumns = {NotesTable.COLUMN_ID, NotesTable.COLUMN_GROUP_ID,
			NotesTable.COLUMN_PRIORITY, NotesTable.COLUMN_TITLE, NotesTable.COLUMN_DESCRIPTION
	};
	
	private String[] allGroupColumns = {GroupsTable.COLUMN_ID, GroupsTable.COLUMN_NAME};
	
	private String[] allConfigurationColumns = {ConfigurationTable.COLUMN_ID, ConfigurationTable.COLUMN_SALT, ConfigurationTable.COLUMN_TESTVALUE};
	
	
	public NotesDataSource(Context context) {
		dbHelper = new NotesSQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
		database = null;
	}
	
	public Configuration createConfiguration(String salt, String testvalue) {
		ContentValues values = new ContentValues();
		values.put(ConfigurationTable.COLUMN_SALT, salt);
		values.put(ConfigurationTable.COLUMN_TESTVALUE, testvalue);
		long insertId = database.insert(ConfigurationTable.TABLE_CONFIGURATION, null, values);
		// Query the new note and return a note object
		Cursor cursor = database.query(ConfigurationTable.TABLE_CONFIGURATION, allConfigurationColumns,
				ConfigurationTable.COLUMN_ID + "=" + insertId, null, null, null, null);
		cursor.moveToFirst();
		Configuration config = cursorToConfiguration(cursor);
		cursor.close();
		return config;
	}
	
	public Configuration getConfiguration() {
		Configuration config = null;
		Cursor cursor = database.query(ConfigurationTable.TABLE_CONFIGURATION, allConfigurationColumns,
				null, null, null, null, null);
		// Verify if any rows are returned
		if (cursor.getCount() < 1) {
			return config; // null
		}
		cursor.moveToFirst();
		config = cursorToConfiguration(cursor);
		cursor.close();
		return config;
	}
	
	public void deleteConfiguration() {
		database.delete(ConfigurationTable.TABLE_CONFIGURATION, null, null);
	}
	
	public Note createNote(String title, String description, int priority, long group_id){
		// Set the values
		ContentValues values = new ContentValues();
		values.put(NotesTable.COLUMN_TITLE, title);
		values.put(NotesTable.COLUMN_DESCRIPTION, description);
		if (group_id < 1) {group_id = 1; } // Set default group if not specified
		values.put(NotesTable.COLUMN_GROUP_ID, group_id);
		values.put(NotesTable.COLUMN_PRIORITY, priority);
		
		// Insert the new note
		long insertId = database.insert(NotesTable.TABLE_NOTES, null, values);
		
		// Query the new note and return a note object
		Cursor cursor = database.query(NotesTable.TABLE_NOTES, allNoteColumns,
				NotesTable.COLUMN_ID + "=" + insertId, null, null, null, null);
		cursor.moveToFirst();
		Note newNote = cursorToNote(cursor);
		cursor.close();
		return newNote;
	}
	
	public void updateNote(Note note) {
		String whereClause = "_id=" + note.getId();
		ContentValues values = new ContentValues();
		values.put(NotesTable.COLUMN_GROUP_ID, note.getGroupId());
		values.put(NotesTable.COLUMN_DESCRIPTION, note.getDescription());
		values.put(NotesTable.COLUMN_TITLE, note.getTitle());
		values.put(NotesTable.COLUMN_PRIORITY, note.getPriority());
		values.put(NotesTable.COLUMN_ID, note.getId());
		database.update(NotesTable.TABLE_NOTES, values, whereClause, null);
	}
	
	public void updateGroup(Group group) {
		String whereClause = "_id=" + "=" + group.getId();
		ContentValues values = new ContentValues();
		values.put(GroupsTable.COLUMN_NAME, group.getName());
		database.update(GroupsTable.TABLE_GROUPS, values, whereClause, null);
	}
	
	public Group createGroup(String name) {
		// Set the values
		ContentValues values = new ContentValues();
		values.put(GroupsTable.COLUMN_NAME, name);
		
		// Insert the new Group
		long insertId = database.insert(GroupsTable.TABLE_GROUPS, null, values);
		
		// Query the new group and return a group object
		Cursor cursor = database.query(GroupsTable.TABLE_GROUPS, allGroupColumns, 
				GroupsTable.COLUMN_ID + "=" + insertId, null, null, null, null);
		cursor.moveToFirst();
		Group newGroup = cursorToGroup(cursor);
		cursor.close();
		return newGroup;
	}
	
	public void deleteNote(Note note) {
		long id = note.getId();
		database.delete(NotesTable.TABLE_NOTES, NotesTable.COLUMN_ID + "=" + id, null);
	}
	
	public void deleteAllNotes() {
		database.delete(NotesTable.TABLE_NOTES, null, null);
	}
	
	public void deleteAllGroups() {
		database.delete(GroupsTable.TABLE_GROUPS, null, null);
	}
	
	// Deletes all notes belonging to the supplied group ID
	public void deleteNotesByGroup(long groupID) {
		database.delete(NotesTable.TABLE_NOTES, NotesTable.COLUMN_GROUP_ID + "=" + groupID, null);
	}
	
	public void deleteGroup(Group group) {
		long id = group.getId();
		database.delete(GroupsTable.TABLE_GROUPS, GroupsTable.COLUMN_ID + "=" + id, null);
	}
	
	public List<Note> getAllNotes() {
		List<Note> notes = new ArrayList<Note>();
		// Note, can't sort here as all data is encrypted
		Cursor cursor = database.query(NotesTable.TABLE_NOTES, allNoteColumns, null, null, null, null,  null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Note note = cursorToNote(cursor);
			notes.add(note);
			cursor.moveToNext();
		}
		cursor.close();
		return notes;
	}
	
	public List<Note> getNotesInGroup(long gID) {
		List<Note> notes = new ArrayList<Note>();
		String whereClause = NotesTable.COLUMN_GROUP_ID + "=" + gID;
		// Note, can't sort here as all data is encrypted
		Cursor cursor = database.query(NotesTable.TABLE_NOTES, allNoteColumns, whereClause, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Note note = cursorToNote(cursor);
			notes.add(note);
			cursor.moveToNext();
		}
		return notes;
	}
	
	public List<Group> getAllGroups() {
		List<Group> groups = new ArrayList<Group>();
		// Note, can't sort here as all data is encrypted
		Cursor cursor = database.query(GroupsTable.TABLE_GROUPS, allGroupColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Group group = cursorToGroup(cursor);
			groups.add(group);
			cursor.moveToNext();
		}
		cursor.close();
		return groups;
	}
	
	public Group getGroupByID(long groupID) {
		Cursor cursor = database.query(GroupsTable.TABLE_GROUPS, allGroupColumns,
				GroupsTable.COLUMN_ID + "=" + groupID, null, null, null, null);
		cursor.moveToFirst();
		Group newGroup = cursorToGroup(cursor);
		cursor.close();
		return newGroup;
	}
	
	public Note getNoteByID(long noteID) {
		// Query the new note and return a note object
		Cursor cursor = database.query(NotesTable.TABLE_NOTES, allNoteColumns,
				NotesTable.COLUMN_ID + "=" + noteID, null, null, null, null);
		cursor.moveToFirst();
		Note newNote = cursorToNote(cursor);
		cursor.close();
		return newNote;
	}
	
	public void addDefaultGroup(SecretKey key) 
			throws  InvalidKeyException, 
					NoSuchAlgorithmException, 
					NoSuchPaddingException, 
					InvalidAlgorithmParameterException, 
					IllegalBlockSizeException, 
					BadPaddingException, 
					UnsupportedEncodingException {
		// Add default group, encrypted of course
		String encryptedGroup = SymmetricKeyLib.encryptString("Default", key);
		ContentValues values = new ContentValues();
		values.put(GroupsTable.COLUMN_NAME, encryptedGroup);
		database.insert(GroupsTable.TABLE_GROUPS, null, values);
	}
	
	private Note cursorToNote(Cursor cursor) {
		Note note = new Note();
		note.setId(cursor.getLong(0));
		note.setGroupId(cursor.getLong(1));
		note.setPriority(cursor.getInt(2));
		note.setTitle(cursor.getString(3));
		note.setDescription(cursor.getString(4));
		return note;
	}
	
	private Configuration cursorToConfiguration(Cursor cursor) {
		Configuration config = new Configuration();
		config.setId(cursor.getLong(0));
		config.setSalt(cursor.getString(1));
		config.setTestValue(cursor.getString(2));
		return config;
	}
	
	private Group cursorToGroup(Cursor cursor) {
		Group group = new Group();
		group.setId(cursor.getLong(0));
		group.setName(cursor.getString(1));
		return group;
	}
}
