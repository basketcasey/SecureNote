package basketcasey.com.androidsecurenote;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.*;
import basketcasey.com.androidsecurenote.cryptography.CryptoStore;
import basketcasey.com.androidsecurenote.cryptography.SymmetricKeyLib;
import basketcasey.com.androidsecurenote.database.*;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class NoteList extends Activity {
	public final String LOG_TAG = "NoteList";
	CryptoStore cryptoStore = null;
	Context context = null;
	private NotesDataSource datasource;
	private long groupId = 0; // Used to send intent when new note requested in selected group
	private ArrayAdapter<CharSequence> groupDataAdapter;
	private Spinner groupSpinner = null;
	ListView noteListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note_list);
		context = getApplicationContext();

		Intent intent = getIntent();
		cryptoStore = (CryptoStore) intent.getSerializableExtra("CryptoObject");
		Bundle b = intent.getExtras();
		if(b!=null)
		{
            if (b.containsKey("MESSAGE")) {
                groupId = (Long) b.get("MESSAGE");
            }
		}
		// Block Android for taking pictures of the app when backgrounded, can leak sensitive info
		// Has to happen before setContentView
	    getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
	    
		noteListView = (ListView) findViewById(R.id.notes_listview);
		
		datasource = new NotesDataSource(this);
		datasource.open();
		
		List<Group> groupList = datasource.getAllGroups();
		// Decrypt the group names
		ListIterator<Group> li = groupList.listIterator();
		while(li.hasNext()){
			int index = li.nextIndex();
			li.next(); // Increment the iterator
			Group g = groupList.get(index);
			String decrypted = null;
			try {
				decrypted = SymmetricKeyLib.decryptString(g.getName(), cryptoStore.getKey());
			} catch (Exception e) {
				Log.e(LOG_TAG, "Unable to decrypt string");
				decrypted = "ERROR";
			}
			
			g.setName(decrypted);
			groupList.set(index, g);
		}
		
		// Now that the groups are decrypted, they can be sorted
		Collections.sort(groupList, new CustomGroupComparator());

		// Populate the group spinner
        groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        groupDataAdapter = new ArrayAdapter(this, R.layout.group_spinner_item, groupList);
        groupDataAdapter.setDropDownViewResource(R.layout.spinner_item);
        groupSpinner.setAdapter(groupDataAdapter);

        // Create event handler to display notes in the selected group
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            	Spinner spinner = (Spinner) parent;
                Group grp = (Group) spinner.getSelectedItem();
                groupId = grp.getId();
            	populateNotesListview(groupId);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            	groupId = 0;
            }
        });


        // Add event handler to open note details when a note is selected
        noteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		      public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
		        final Note selNote = (Note) parent.getItemAtPosition(position);
					openNoteDetailsActivity(selNote.getId(), selNote.getGroupId());
		      }
		    });

	}

	// Used by database helper to get the public key for encrypting the default group
	// created when the database is created on the first run
	public SecretKey getSecretKey() {
		return cryptoStore.getKey();
	}
	
	public void populateNotesListview(long gID) {
		List<Note> noteList = datasource.getNotesInGroup(gID);
		
		// decrypt the titles in the list
		ListIterator<Note> i = noteList.listIterator();
		while (i.hasNext()) {
			Note n = i.next();
			String title = null;
			try {
				title = SymmetricKeyLib.decryptString(n.getTitle(), cryptoStore.getKey());
			} catch (Exception e) {
				title = "Error";
			}
			n.setTitle(title);
		}
		
		// Now that the notes are decrypted, they can be sorted
		Collections.sort(noteList, new CustomNoteComparator());
		
		// Populate the listview spinner with notes
		noteListView = (ListView) findViewById(R.id.notes_listview);
        ArrayAdapter<CharSequence> noteDataAdapter =  new ArrayAdapter(this, 
                R.layout.simple_list_item, noteList);
        noteListView.setAdapter(noteDataAdapter);
	}
	
	public void openNoteDetailsActivity(long noteId, long groupId) {
		Intent intent = new Intent(this, NoteDetails.class);
		intent.putExtra("MESSAGE", noteId);
		intent.putExtra("MESSAGE2", groupId);
		intent.putExtra("CryptoObject", cryptoStore);
		startActivity(intent);
		finish(); // Need to finish this activity when opening NoteDetails, that activity starts new intent
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.note_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		 case R.id.action_addnote:
			 openNoteDetailsActivity(0, groupId); // Zero ID means add new vs. open existing
		 return true;
		 case R.id.action_groupmgr:
			 openGroupManagerActivity();
		 return true;
		 case R.id.action_wipeall:
			 wipeAllData();
		 return true;
		 case R.id.action_changePassword:
			 promptForNewPassword();
		 return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	public void openGroupManagerActivity() {
		Intent intent = new Intent(this, GroupManager.class);
		intent.putExtra("CryptoObject", cryptoStore);
		startActivity(intent);
		
	}
	
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    
	    // Need to update the Group spinner and the list of notes in the groups
		List<Group> groupList = datasource.getAllGroups();
		// Decrypt the group names
		ListIterator<Group> li = groupList.listIterator();
		while(li.hasNext()){
			int index = li.nextIndex();
			li.next(); // Increment the iterator
			Group g = groupList.get(index);
			String decrypted = null;
			try {
				decrypted = SymmetricKeyLib.decryptString(g.getName(), cryptoStore.getKey());
			} catch (Exception e) {
				decrypted = "Error";
			}
			
			g.setName(decrypted);
			groupList.set(index, g);
		}
		
		// Now that the groups are decrypted, they can be sorted
		Collections.sort(groupList, new CustomGroupComparator());
		
		// Populate the group spinner
		groupDataAdapter.clear();
        groupDataAdapter =  new ArrayAdapter(this, android.R.layout.simple_spinner_item, groupList);
        groupSpinner.setAdapter(groupDataAdapter);

        // if gID != 0, set group spinner as appropriate
        // This means the user hit back button in note details and is returning
        // to currently opened group
        if (groupId > 0) {
            int position = 0;
            Iterator<Group> i = groupList.iterator();
            while (i.hasNext()) {

                Group g = i.next();
                if (g.getId()== groupId) {
                    groupSpinner.setSelection(position);
                    populateNotesListview(groupId);
                }
                position++;
            }
        } else {
            // Populate list of notes
            populateNotesListview(groupId);
        }
	}
	
	private void encryptAllWithNewKey(SecretKey oldKey, SecretKey newKey) {
		try {
			// Update Notes table - NotesTable.COLUMN_TITLE, NotesTable.COLUMN_DESCRIPTION 
			List<Note> noteList = datasource.getAllNotes();
			ListIterator<Note> liNote = noteList.listIterator();
			while (liNote.hasNext()) { // foreach note
				Note note = liNote.next();
				// decrypt/encrypt title and description
				String decryptedTitle = SymmetricKeyLib.decryptString(note.getTitle(), oldKey);
				String decryptedDesc = SymmetricKeyLib.decryptString(note.getDescription(), oldKey);
				String encryptedTitle = SymmetricKeyLib.encryptString(decryptedTitle, newKey);
				String encryptedDesc = SymmetricKeyLib.encryptString(decryptedDesc, newKey);
				note.setTitle(encryptedTitle);
				note.setDescription(encryptedDesc);
				// 	update row
				datasource.updateNote(note);
			}
			
			// Now that the groups are decrypted, they can be sorted
			Collections.sort(noteList, new CustomNoteComparator());
			
			//Update Groups table - GroupsTable.COLUMN_NAME
			List<Group> groupList = datasource.getAllGroups();
			ListIterator<Group> liGroup = groupList.listIterator();
			while(liGroup.hasNext()){ // foreach group
				Group group = liGroup.next();
				//decrypt/encrypt group name with old key
				String decrypted = SymmetricKeyLib.decryptString(group.getName(), oldKey);
				String encrypted = SymmetricKeyLib.encryptString(decrypted, newKey);
				group.setName(encrypted);
				// update row
				datasource.updateGroup(group);
			}
		} catch (Exception e) {
			// Error occurred updating the database with new symmetric key
		}
	}

	private void updatePassword(String newPassword) {
		SecretKey oldkey = cryptoStore.getKey();
		// Store new password
		SecretKey newkey = null;
		try {
			String salt = SymmetricKeyLib.genBase64Salt();
			newkey = SymmetricKeyLib.genKeyWithSaltPassword(newPassword, salt);
			// Encrypt the string VALID with the generated key, this is used later to verify the users password.
			// The generated key must decrypt this testvalue to the string "VALID" to pass
			String testvalue = SymmetricKeyLib.encryptString(SecureNoteMain.PasswordTestString, newkey);
			// Update configuration entry used to validate the password
			datasource.deleteConfiguration();
			datasource.createConfiguration(salt, testvalue);
		} catch (Exception e) {
			return;
		}
		// Overwrite password variable to keep out of RAM
		newPassword = "xxxxxxx";
		cryptoStore.setKey(newkey);
		// Update all encrypted data
		encryptAllWithNewKey(oldkey, newkey);
	}
	
	private void wipeAllData() {
		// Prompt confirmation dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.deleteall)
		       .setCancelable(false)
		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   // Delete everything from the database
		        	   datasource.deleteAllNotes();
			    	   datasource.deleteAllGroups();
			    	   datasource.deleteConfiguration();

			    	   // Exit application
			    	   finish();
			    	   System.exit(0);
		           }
		       })
		       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void promptForNewPassword() {
	   AlertDialog.Builder builder = new AlertDialog.Builder(this);

	   // Get the layout inflater
	   LayoutInflater linf = LayoutInflater.from(this);
	    // Pass null as the parent view because its going in the dialog layout
	   final View inflater = linf.inflate(R.layout.change_password, null);

 	   builder.setView(inflater);
 	   
	   final EditText et1 = (EditText) inflater.findViewById(R.id.et_enter_old_password);
 	   final EditText et2 = (EditText) inflater.findViewById(R.id.et_enter_new_password);
 	   final EditText et3 = (EditText) inflater.findViewById(R.id.et_reenter_new_password);
 	   
	   // Add action buttons
	   builder.setPositiveButton(R.string.changePassword, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   // validate password

	            	   // check for null before getting text

	            	   String oldpw = et1.getText().toString();
	            	   String newpw = et2.getText().toString();
	            	   String confpw = et3.getText().toString();
	            	   
	            	   if (validatePassword(oldpw, newpw, confpw)) {
	            		   // Password checks out, call routine to update encrypted data in the db and
	            		   // change the password configuration information
	            		   updatePassword(newpw);
	            		   dialog.dismiss();
	            	   }
	            	   // If some validation valid, it will toast.  Reprompt with a new dialog.
	               }
	           })
	           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   dialog.cancel();
	               }
	           });      
	    AlertDialog alert = builder.create();
	    alert.show();
	}
	
	private boolean validatePassword(String oldpw, String newpw, String confpw) {
		// Check for nulls
		if (oldpw == null || newpw == null || confpw == null) {
			Toast.makeText(this, "Passwords are required", Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure password and retypted passwords match
		if (!newpw.contentEquals(confpw)) {
			Toast.makeText(this, "Passwords must match", Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure password meets complexity
		if (newpw.length() < 7) { // min length of 7 chars
			Toast.makeText(this, "Password must be at least 7 characters long", 
					Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure new password is different from old
		if (newpw.equals(oldpw)) { // min length of 7 chars
			Toast.makeText(this, "Old and new passwords are the same.", 
					Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure old password is correct
		boolean bKeyValid = false;
		try {
			// validate the password by generating a symmetric key from the supplied password and decrypting a known string
			Configuration conf = datasource.getConfiguration();
			SecretKey key = SymmetricKeyLib.genKeyWithSaltPassword(oldpw, conf.getSalt());
			
			// Test the generated key by decrypting data that was previously encrypted with the key
			// and stored for the purpose of validating credentials
			String testValue = SymmetricKeyLib.decryptString(conf.getTestValue(), key);
			if (testValue.contentEquals(SecureNoteMain.PasswordTestString)) {
				bKeyValid = true;
			}
		} catch (Exception e) {
			bKeyValid = false;
			return false;
		}
		// verify result of old password test passed
		if (bKeyValid == false) {
			Toast.makeText(this, "Old password is incorrect", Toast.LENGTH_LONG).show();
			return false;			
		}
	
		return true;
	}

	// Handle back button
	@Override
	public void onBackPressed() {
		// If the user is not in the default group (top on list), open the default group otherwise exit

		// get selected group id to return to currently selected group
		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
		Group selGroup = (Group)groupSpinner.getSelectedItem();
		long gID = selGroup.getId();
		int pos = groupSpinner.getSelectedItemPosition();

		if (pos == 0) {
			finish();
		} else {
			// open default group
			groupSpinner.setSelection(0);
		}
	}
}