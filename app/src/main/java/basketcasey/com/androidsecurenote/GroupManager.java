package basketcasey.com.androidsecurenote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import basketcasey.com.androidsecurenote.database.NotesDataSource;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class GroupManager extends Activity {
	private static final String LOG_TAG = "GroupManager";
	private NotesDataSource datasource;
	private List<Group> groupList = null;
	private Context context;
	private CryptoStore cryptoStore = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_manager);
		// Block Android for taking pictures of the app when backgrounded, can leak sensitive info
	    getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
		context = this.getApplicationContext();
	    
		Intent intent = getIntent();
		cryptoStore = (CryptoStore) intent.getSerializableExtra("CryptoObject");
		
		datasource = new NotesDataSource(this);
		datasource.open();
		
		groupList = datasource.getAllGroups();
		// Decrypt the group names
		ListIterator<Group> li = groupList.listIterator();
		while(li.hasNext()){
			Group g=li.next();
			String decrypted = null;
			try {
				decrypted = SymmetricKeyLib.decryptString(g.getName(), cryptoStore.getKey());
			} catch (Exception e) {
				Log.e(LOG_TAG, "Error decrypting group name");
				decrypted = "Error";
			}
			g.setName(decrypted);
		}
		
		// Now that the groups are decrypted, they can be sorted
		Collections.sort(groupList, new CustomGroupComparator());
		
		// Populate the group spinner
        Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        ArrayAdapter<CharSequence> groupDataAdapter =  new ArrayAdapter(this,
                R.layout.group_spinner_item, groupList);
        groupDataAdapter.setDropDownViewResource(R.layout.spinner_item);
        groupSpinner.setAdapter(groupDataAdapter);
 
        // Create event handler that changes title on selection of an existing group
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            	Spinner spinner = (Spinner) parent;
            	String selectedGroup = spinner.getSelectedItem().toString();
            	
            	setGroupNameField(selectedGroup);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });  
        
        // User requesting to create a new group
        final Button createBtn = (Button) findViewById(R.id.btn_createnew);
        createBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Get user supplied group name
            	String name = getUserSuppliedGroupName();
        		if (name.length() < 1) {
        			Toast.makeText(context,  "Group name can't be blank", Toast.LENGTH_LONG).show();
        			return;
        		}
        		
        		// Make sure group name is unique
        		Iterator<Group> i = groupList.iterator();
        		while (i.hasNext()) {
        			Group g = i.next();
        			if ( g.getName().equals(name.trim()) ) {
        				// Toast error
        				Toast.makeText(context,  "Group name already exists", Toast.LENGTH_LONG).show();
        				return;
        			}
        		}
        		
            	// Insert new group into the database
        		String encName = null;
        		try {
        			encName = SymmetricKeyLib.encryptString(name, cryptoStore.getKey());
        		} catch (Exception e) {
        			Log.e(LOG_TAG, "Error occurred encrypting group name");
        			encName = "Error";
        		}
            	datasource.createGroup(encName);
            	// Relaunch this activity
            	relaunchGroupManagerActivity();
            }
        });
        
        // User updating the name of the selected group
        final Button updateBtn = (Button) findViewById(R.id.btn_update);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Get selected group (mainly need the ID for update statement)
        		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        		Group selGroup = (Group)groupSpinner.getSelectedItem();
 
        		// Get user supplied group name
        		String name = getUserSuppliedGroupName();
        		if (name.length() < 1) {
        			// Toast error
        			return;
        		}
        		// Encrypt name for storage in the DB
        		String encName = null;
        		try {
        			encName = SymmetricKeyLib.encryptString(name, cryptoStore.getKey());
        		} catch (Exception e) {
        			Log.e(LOG_TAG, "Error occurred encrypting group name");
        			encName = "Error";
        		}
        		selGroup.setName(encName);
        		
        		// Update the database
        		datasource.updateGroup(selGroup);
        		
        		// Relaunch this activity (how go back to main? using back button?)
        		relaunchGroupManagerActivity();
            }
        });
        
        // User is deleting a group
        final Button deleteBtn = (Button) findViewById(R.id.btn_delete);
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        		Spinner groupSpinner = (Spinner) findViewById(R.id.group_spinner);
        		Group selGroup = (Group)groupSpinner.getSelectedItem();
        		
        		// Before deleting, need to make sure there will be at least one group left after deletion
        		List<Group> groups = datasource.getAllGroups();
        		if (groups.size() < 2) {
        			// Can't perform option until another group is added first
        			String msg = "Cannot delete the last group, at least one group must exist.  Create a new group first.";
        			Toast.makeText(context,  msg, Toast.LENGTH_LONG).show();
        			return;
        		}
        		 doDelete(selGroup);

            }
        });
	}

	private void doDelete(final Group selGroup) {
		// Confirm delete
		// Prompt confirmation dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.confGrpDelete)
		       .setCancelable(false)
		       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
			       		// Delete all notes stored in this group
			       		datasource.deleteNotesByGroup(selGroup.getId());
			       		
			       		// Delete this group
			       		datasource.deleteGroup(selGroup);
			       		
			       		// Relaunch this activity
			       		relaunchGroupManagerActivity();
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
	
	// Gets user entered group name and performs input validation
	private String getUserSuppliedGroupName() {
		EditText nameField = (EditText) findViewById(R.id.groupname_edittext);
		Editable groupName = nameField.getText();
		if (groupName.length() > 30) {
			
			String msg = "Shortening to max of 30 characters";
			Toast.makeText(context,  msg, Toast.LENGTH_LONG).show();
			CharSequence cs = null;
			if (groupName.length() < 15) {
				cs = groupName.subSequence(0, groupName.length() - 1);	
			} else {
				cs = groupName.subSequence(0, 30);
			}
			return cs.toString();
		}
		if (groupName.length() < 1) {
			return "";
		}
		return groupName.toString();
	}
	
	// Set the name field on the form (called from group spinner select event)
	private void setGroupNameField(String name) {
		EditText nameField = (EditText) findViewById(R.id.groupname_edittext);
		nameField.setText(name);
	}
	
	private void relaunchGroupManagerActivity() {
		Intent intent = new Intent(this, GroupManager.class);
		intent.putExtra("CryptoObject", cryptoStore);
		
		startActivity(intent);
		// Finish the current activity so it doesn't remain on the back button stack.
		// This way if the user hits back they return to the main activity not this one.
		finish(); 
	}
}
