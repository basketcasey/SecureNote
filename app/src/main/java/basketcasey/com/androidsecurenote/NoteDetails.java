package basketcasey.com.androidsecurenote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.*;
import basketcasey.com.androidsecurenote.cryptography.CryptoStore;
import basketcasey.com.androidsecurenote.cryptography.SymmetricKeyLib;
import basketcasey.com.androidsecurenote.database.CustomGroupComparator;
import basketcasey.com.androidsecurenote.database.Group;
import basketcasey.com.androidsecurenote.database.Note;
import basketcasey.com.androidsecurenote.database.NotesDataSource;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class NoteDetails extends Activity {
	private NotesDataSource datasource;
	private CryptoStore cryptoStore = null;
	private static final String LOG_TAG = "NoteDetails";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note_details);
		
		// Block Android for taking pictures of the app when backgrounded, can leak sensitive info
	    getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
	    
        // Get noteID passed in with intent - determines if note is being added or edited
        long nID = 0;
        long gID = 0;
		Intent intent = getIntent();
		cryptoStore = (CryptoStore) intent.getSerializableExtra("CryptoObject");
        Bundle b = intent.getExtras();
        if(b!=null)
        {
            nID =(Long) b.get("MESSAGE");
            gID =(Long) b.get("MESSAGE2");
        }
        final long noteID = nID; // Create final version of noteID var, needed for inner classes
	    final long groupID = gID;
	    
		datasource = new NotesDataSource(this);
		datasource.open();
		
		// Get group list from database to populate the spinner
		List<Group> groupList = datasource.getAllGroups();
		// Decrypt the group names
		ListIterator<Group> li = groupList.listIterator();
		while(li.hasNext()){
			Group g=li.next();
			String decrypted = null;
			try {
				decrypted = SymmetricKeyLib.decryptString(g.getName(), cryptoStore.getKey());
			} catch (Exception e) {
				Log.e(LOG_TAG, "Error decrypting group names");
				decrypted = "Error";
			}
			g.setName(decrypted);
		}
		
		// Now that the groups are decrypted, they can be sorted
		Collections.sort(groupList, new CustomGroupComparator());
		
		// Populate the group spinner
        Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        ArrayAdapter<CharSequence> groupDataAdapter =  new ArrayAdapter(this, 
                android.R.layout.simple_spinner_item, groupList);
        groupDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupDataAdapter);
	    
        // if groupID != 0, set group spinner as appropriate
        if (groupID > 0) {
        	int position = 0;
        	Iterator<Group> i = groupList.iterator();
    		while (i.hasNext()) {
    			
    			Group g = i.next();
    			if (g.getId()== groupID) {
    				groupSpinner.setSelection(position);		
    			}
    			position++;
    		}
        }
        
	    // if noteID == 0, creating a new note, otherwise opening an existing note
		if (noteID > 0) {
			// Get selected note from the database
			Note dispNote = datasource.getNoteByID(noteID);
			
			// Decrypt the note title and description
			String title = null;
			String description = null;
			try {
				title = SymmetricKeyLib.decryptString(dispNote.getTitle(), cryptoStore.getKey());
				description = SymmetricKeyLib.decryptString(dispNote.getDescription(), cryptoStore.getKey());
			} catch (Exception e){
				Log.e(LOG_TAG, "Error decrypting title and description");
				title = "Error";
				description = "Error";
			}
			
			// Populate the title field
        	EditText titleObj = (EditText) findViewById(R.id.title_edittext);
        	titleObj.setText(title);
        	// Populate the description field
        	EditText descObj = (EditText) findViewById(R.id.desc_edittext);
        	descObj.setText(description);
		}
		
        final Button saveBtn = (Button) findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Get user input
        		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        		Group selGroup = (Group)groupSpinner.getSelectedItem();
            	EditText titleObj = (EditText) findViewById(R.id.title_edittext);
            	EditText descObj = (EditText) findViewById(R.id.desc_edittext);
        		long gID = selGroup.getId();
        		Editable titleText = titleObj.getText();
        		String title = " ";
        		if (titleText.length() > 0) {
        			title = titleText.toString();
        		}
        		
        		Editable descText = descObj.getText();
        		String desc = " ";
        		if (descText.length() > 0) {
        			desc = descText.toString();
        		}
        		int priority = 0; // Future enhancement variable
        		
        		// Encrypt title and description
        		try {
    				title = SymmetricKeyLib.encryptString(title, cryptoStore.getKey());
    				desc = SymmetricKeyLib.encryptString(desc, cryptoStore.getKey());
    			} catch (Exception e){
    				Log.e(LOG_TAG, "Error encrypting title and description");
    				title = "Error";
    				desc = "Error";
    			}
        		
            	// Determine if doing a save or update
            	if (noteID == 0) { // Doing add operation
            		// Store record in database
            		datasource.createNote(title, desc, priority, gID);
            		// Return to main activity passing in group ID for sorting reasons
            		 openNoteMainActivity(gID);
            	} else { // Doing update operation
                    // Update record in DB
            		
            		// Create new note object with fields gathered from the UI
            		Note updNote = new Note();
            		updNote.setDescription(desc);
            		updNote.setGroupId(gID);
            		updNote.setPriority(0);
            		updNote.setTitle(title);
            		updNote.setId(noteID);
            		
            		// Update the existing note in the database
            		datasource.updateNote(updNote);
            		
                	// Launch NoteMain activity passing in groupID to display
            		openNoteMainActivity(gID);
            	}
            }
        });
        
        final Button deleteBtn = (Button) findViewById(R.id.btn_delete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if note ID is zero, the user asked to add a new note and wants to delete it
            	// The note was never saved so just launch NoteMain activity with current group selected
            	if (noteID == 0) {
            		Toast msg = Toast.makeText(getBaseContext(), 
            				"New note was never saved, no need to delete", Toast.LENGTH_LONG);
            		msg.show();
            	} else { // delete note by noteID
            		// Delete the selected note
            		Note delNote = datasource.getNoteByID(noteID);
            		datasource.deleteNote(delNote);
            		// launch NoteMain activity with current group selected
            		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
            		Group selGroup = (Group)groupSpinner.getSelectedItem();
            		long gID = selGroup.getId();
            		openNoteMainActivity(gID);
            	}
            }
        });
	}

	// Launches main activity with the selected group parameter for display sorting
	public void openNoteMainActivity(long gID) {
		Intent intent = new Intent(this, NoteList.class);
		intent.putExtra("MESSAGE", gID);
		intent.putExtra("CryptoObject", cryptoStore);
		Log.d("SecureNote", "Starting Intent");
		startActivity(intent);
		finish(); // Finish this activity to remove from the back button stack
	}
	
	// Handle back button, since there wont be history the app will just exit
	@Override
	public void onBackPressed() {
		// get selected group id to return to currently selected group
		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
		Group selGroup = (Group)groupSpinner.getSelectedItem();
		long gID = selGroup.getId();
		Log.d("SecureNote", "Back Pressed" + gID);
		openNoteMainActivity(gID);
	}
}
