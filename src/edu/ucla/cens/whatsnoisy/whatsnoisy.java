package edu.ucla.cens.whatsnoisy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;

import edu.ucla.cens.whatsnoisy.data.LocationDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.services.LocationService;
import edu.ucla.cens.whatsnoisy.services.LocationTrace;
import edu.ucla.cens.whatsnoisy.services.LocationUpload;
import edu.ucla.cens.whatsnoisy.services.SampleUpload;
import edu.ucla.cens.whatsnoisy.Record;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;

public class whatsnoisy extends Activity {

	// An arbitrary constant to pass to the GoogleLoginHelperService
	private static final int GET_ACCOUNT_REQUEST = 1;
	private static final int RECORD_FINISHED = 0;
	private static final String TAG = "whatsnoisy";
	SharedPreferences preferences;
	private SampleDatabase sdb;
	private LocationDatabase ldb;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		preferences = this.getSharedPreferences(Settings.NAME, Activity.MODE_PRIVATE);

		updateAuthToken();

	}
	
	private void startServices() {
		Intent service = new Intent();

		service.setClass(this, LocationService.class);
		startService(service);
		
		service.setClass(this, LocationTrace.class);
		startService(service);
		
		service.setClass(this, LocationUpload.class);
		startService(service);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stopServices();
	}
	
	private void stopServices() {
		Intent service = new Intent();

		Log.d(TAG,"Stop location service");
		
		service.setClass(this, LocationService.class);
		stopService(service);
		
//		service.setClass(this, LocationTrace.class);
//		stopService(service);
//		
//		service.setClass(this, LocationUpload.class);
//		stopService(service);
	}


	private class AuthorizeTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... progress) {
			return authorize();
		}

		protected void onPostExecute(Boolean result) {
			//save if we have been authenticated or not
			preferences.edit().putBoolean("authenticated", result).commit();
			
			if(result) {
				Log.d(TAG, "authorized");
				
				startServices();


				sdb = new SampleDatabase(whatsnoisy.this);
				sdb.openRead();
				if(sdb.hasSamples()) {
					Log.d(TAG,"sample database has samples");
					Intent uploadService = new Intent(whatsnoisy.this, SampleUpload.class);
					whatsnoisy.this.startService(uploadService);
				}
				sdb.close();
				
				ldb = new LocationDatabase(whatsnoisy.this);
				ldb.open();
				if(ldb.hasSamples()) {
					Log.d(TAG,"location database has locations");
					Intent uploadService = new Intent(whatsnoisy.this, LocationUpload.class);
					whatsnoisy.this.startService(uploadService);
				}
				ldb.close();
				
				//start recording intent
				Intent act = new Intent(whatsnoisy.this, Record.class);
				whatsnoisy.this.startActivityForResult(act, RECORD_FINISHED);


			} else {
				AlertDialog alert = new AlertDialog.Builder(whatsnoisy.this)
				.setTitle("Could Not Authenticate")
				.setMessage("Whats Noisy Could Not authenticate with Google. Please make sure this phone is linked to a google account and try again. You can continue but samples will not be uploaded until you are authenticated.")					
				.setPositiveButton("Continue", new OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						//start recording intent
						Intent act = new Intent(whatsnoisy.this, Record.class);
						whatsnoisy.this.startActivityForResult(act, RECORD_FINISHED);
					}})
				.setNegativeButton(android.R.string.cancel, new OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}})
				.create();

				alert.show();
			}




			
		}

	}


	protected boolean authorize() {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		String auth = preferences.getString("AUTHTOKEN", "");
		if(auth=="") {
			return false;
		}

		String base_url = getString(R.string.base_url);

		HttpGet auth_request = new HttpGet(base_url + "/_ah/login?continue=" + base_url + "&auth="+auth);
		HttpResponse response;


		try {
			response = httpClient.execute(auth_request);
			int status = response.getStatusLine().getStatusCode();

			Log.d(TAG,"auth status: "+status);
			if(status == HttpStatus.SC_OK) {

				CookieStore cs = httpClient.getCookieStore();
				Editor edit = preferences.edit();
				edit.putString("AUTHCOOKIE", cs.getCookies().get(0).getValue());
				edit.commit();

				return true;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	protected void updateAuthToken() {
		Bundle bundle = new Bundle();
		bundle.putCharSequence("optional_message", "no optional message?");
		GoogleLoginServiceHelper.getCredentials(
				this,
				GET_ACCOUNT_REQUEST,
				bundle,
				GoogleLoginServiceConstants.PREFER_HOSTED,
				"ah",
				true);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == GET_ACCOUNT_REQUEST) {
			if (resultCode == RESULT_OK) {
				if (intent != null) {
					Bundle extras = intent.getExtras();
					if (extras != null) {

						preferences.edit().putString("AUTHTOKEN", extras.getString(GoogleLoginServiceConstants.AUTHTOKEN_KEY)).commit();

						new AuthorizeTask().execute();

					}
				}
			} 
		} else if (requestCode == RECORD_FINISHED) {
			finish();
		}
	}
}