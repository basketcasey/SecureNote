package basketcasey.com.androidsecurenote;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import basketcasey.com.androidsecurenote.cryptography.CryptoStore;
import basketcasey.com.androidsecurenote.cryptography.SymmetricKeyLib;
import basketcasey.com.androidsecurenote.database.Configuration;
import basketcasey.com.androidsecurenote.database.NotesDataSource;

import javax.crypto.SecretKey;


public class SecureNoteMain extends FragmentActivity implements GenPWFragment.OnPasswordCreatedListener, LoginFragment.OnLoginListener {
	public final String LOG_TAG = "SecureNoteMain";
	public static final String PasswordTestString = "VALID PASSWORD ENTERED";
	Context context = null; // Activity context
    CryptoStore cryptoStore;
    Configuration conf = null; // Contains salt used to generate symmetric key from user password
	private NotesDataSource datasource;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_main);
		context = getApplicationContext();
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    EditText et = (EditText) findViewById(R.id.et_loginmessage);
	    cryptoStore = new CryptoStore();
		datasource = new NotesDataSource(this);
		datasource.open();
	    
	    // Initial application activity will display an authentication
	    // fragment.  Which fragment depends on whether the user has
	    // created a password in the past.
		
		// Determine if a symmetric key salt exists (user already has a password)
		try {
			conf = datasource.getConfiguration();
			
			// If a salt exists, the user has created a symmetric key and just needs to log in
			if (conf != null) {
				et.setText("Please login to continue");
				LoginFragment loginFrag = new LoginFragment();
			    ft.add(R.id.frmlayout_top, loginFrag, "fragmentLogin");
			    ft.commit();
			} else { // User needs to generate a key by creating a password
				et.setText("Please create a password");
				GenPWFragment genPWFrag = new GenPWFragment();
				ft.add(R.id.frmlayout_top, genPWFrag, "fragmentLogin");
			    ft.commit();
			}
		} catch (Exception e) {
			// If something fails, this app needs to crash
			Log.e(LOG_TAG, "Error handing user authentication: " + e.getMessage());
	    	finish();
	    	System.exit(0);
		}
	}

	// Communications interface handling GenPWFragment event where
	// user generates a password
	public void OnPasswordCreated(String password) {
		SecretKey key = null;
		try {
			String salt = SymmetricKeyLib.genBase64Salt();
			key = SymmetricKeyLib.genKeyWithSaltPassword(password, salt);
			// Encrypt the string VALID with the generated key, this is used later to verify the users password.
			// The generated key must decrypt this testvalue to the string "VALID" to pass
			String testvalue = SymmetricKeyLib.encryptString(PasswordTestString, key);
			datasource.createConfiguration(salt, testvalue);
			
			// Need to also add the default group so the spinner doesn't die with null pointer exception
			datasource.addDefaultGroup(key);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to generate symmetric key: " + e.getMessage());
			return;
		}
		// Overwrite password variable to keep out of RAM
		password = "xxxxxxx";
		cryptoStore.setKey(key);
		
		// Launch note activity now
		// Pass in the cryptostore reference with intent.  Use getIntent().getSerializableExtra("MyClass"); on receiver
		Intent intent = new Intent(this, NoteList.class);
		intent.putExtra("CryptoObject", cryptoStore);
		startActivity(intent);
		finish();
		
	}
	
	// Called by login activity to pass the private key password
	public void OnLogin(String password) {
		try {
			// validate the password by generating a symmetric key from the supplied password and decrypting a known string
			boolean bKeyValid = false;
			conf = datasource.getConfiguration();
			SecretKey key = SymmetricKeyLib.genKeyWithSaltPassword(password, conf.getSalt());
			
			// Test the generated key by decrypting data that was previously encrypted with the key
			// and stored for the purpose of validating credentials
			String testValue = SymmetricKeyLib.decryptString(conf.getTestValue(), key);
			if (testValue.contentEquals(PasswordTestString)) {
				bKeyValid = true;
			}
			
			if (!bKeyValid) { 
				// Must be a user-supplied password error
				Log.e(LOG_TAG, "Authentication error, probably incorrect password");
				Toast.makeText(context, "Authentication Error, try again", Toast.LENGTH_LONG).show();
				return;
			} else {
				cryptoStore.setKey(key);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to validate credential: " + e.getMessage());
			Toast.makeText(context, "Authentication Error occurred", Toast.LENGTH_LONG).show();
			return;
		}
		// Continue to secure note.
		Intent intent = new Intent(this, NoteList.class);
		intent.putExtra("CryptoObject", cryptoStore);
		startActivity(intent);
		finish();
	}

}
