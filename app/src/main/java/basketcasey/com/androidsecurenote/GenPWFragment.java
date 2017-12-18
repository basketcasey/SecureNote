package basketcasey.com.androidsecurenote;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class GenPWFragment extends Fragment implements OnClickListener{
	public static final String LOG_TAG = "GenPWFragment";
	private String newPassword = "";
	private View view = null;
	OnPasswordCreatedListener callback;
	
	// Define an interface for communications with main activity
	public interface OnPasswordCreatedListener {
		public void OnPasswordCreated(String password);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.create_password, container, false);
		this.view = view; // Set class variable
		Button btnSubmit = (Button) view.findViewById(R.id.btn_submit);
		btnSubmit.setOnClickListener(this);

		return view;
	}
	
	public void onAttach(Context context) {
		super.onAttach(context);
		// Ensure that the activity implementing this fragment implements our
		// communications interface and initialize the callback variable
		try {
			Activity a;
			if (context instanceof Activity){
				a = (Activity) context;
				callback = (OnPasswordCreatedListener) a;
			}

		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() +
					" must implement OnPasswordCreatedListener");
		}
	}

	public void onClick(View v) {
		Log.i(LOG_TAG, "Click event occurred");
		if (v.getId() == R.id.btn_submit) {
			if (validatePassword()) {
				// Tell parent activity to generate the keystore and keys via interface
				Toast.makeText(view.getContext(), "Processing...", Toast.LENGTH_LONG).show();
				callback.OnPasswordCreated(newPassword);
			}
		}
	}
	
	private boolean validatePassword() {
		// Get user input and validate
		EditText pw = (EditText)view.findViewById(R.id.et_enter_new_password);
		EditText confPW = (EditText)view.findViewById(R.id.et_reenter_new_password);
			
		String passwd = pw.getText().toString();
		String confpw = confPW.getText().toString();
		
		// Check for nulls
		if (passwd == null || confpw == null) {
			Toast.makeText(view.getContext(), "Passwords are required", Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure password and retypted passwords match
		if (!passwd.contentEquals(confpw)) {
			Toast.makeText(view.getContext(), "Passwords must match", Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Ensure password meets complexity
		if (passwd.length() < 7) { // min length of 7 chars
			Toast.makeText(view.getContext(), "Password must be at least 7 characters long", 
					Toast.LENGTH_LONG).show();
			return false;
		}
		
		// Not really sure what the requirements are for private key password complexity yet
		newPassword = passwd; // Set class variable with new password
		return true;
	}
	
}
