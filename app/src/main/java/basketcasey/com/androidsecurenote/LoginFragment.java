package basketcasey.com.androidsecurenote;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
	

public class LoginFragment extends Fragment implements OnClickListener {
	public final String LOG_TAG = "LoginFragment";
	public View view = null;
	OnLoginListener callback;
	
	// Define an interface for communications with main activity
	public interface OnLoginListener {
		public void OnLogin(String password);
	}

	public void onAttach(Context context) {
		super.onAttach(context);
		// Ensure that the activity implementing this fragment implements our
		// communications interface and initialize the callback variable
		try {
			Activity a;
			if (context instanceof Activity) {
				a = (Activity) context;
				callback = (OnLoginListener) a;
			}
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() +
					" must implement OnPasswordCreatedListener");
		}
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.login, container, false);
		Button btnSubmit = (Button) view.findViewById(R.id.btn_submit);
		btnSubmit.setOnClickListener(this);
		return view; 
	}
	
	public void onClick(View v) {
		//Log.i(LOG_TAG, "Click event occurred");
		//Toast.makeText(view.getContext(), "OnClick Event", Toast.LENGTH_LONG).show();
		if (v.getId() == R.id.btn_submit) {
			attemptSubmit();
		}
	}
	
	// Method handles verifying password submitted by the user and calling the parent
	// activity 
	private void attemptSubmit() {
		EditText pwET = (EditText)view.findViewById(R.id.et_enter_new_password);
		String passwd = pwET.getText().toString();
		pwET.setText(""); // Clear password field after user submit attempt
		if (passwd.length() < 7) {
			Toast.makeText(view.getContext(), "Incorrect password", Toast.LENGTH_LONG).show();
			return;
		}
		// verify this password opens KS/Private Key before using callback
		Toast.makeText(view.getContext(), "Processing...", Toast.LENGTH_LONG).show();
		callback.OnLogin(passwd);
		
	}
}
