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

import com.google.android.googlelogin.GoogleLoginServiceBlockingHelper;
import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;
import com.google.android.googlelogin.GoogleLoginServiceNotFoundException;

import edu.ucla.cens.whatsnoisy.data.LocationDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.services.LocationService;
import edu.ucla.cens.whatsnoisy.services.LocationTrace;
import edu.ucla.cens.whatsnoisy.services.LocationTraceUpload;
import edu.ucla.cens.whatsnoisy.services.SampleUpload;
import edu.ucla.cens.whatsnoisy.Record;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
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
	private static final int SHOW_HELP = 2;
	protected static final int START_GPS = 3;
	SharedPreferences preferences;
	private SampleDatabase sdb;
	private LocationDatabase ldb;
	LocationManager manager;
	private String authToken = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		preferences = this.getSharedPreferences(Settings.NAME, Activity.MODE_PRIVATE);
		
		manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

		
		if(preferences.getBoolean("firstboot", true)){
			preferences.edit().putBoolean("firstboot", false).commit();
			startActivityForResult(new Intent(this, Help.class), SHOW_HELP);
		} else if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			buildAlertMessageNoGps();
		} else {
			authUser();
		}
		
	}
	
	  private void buildAlertMessageNoGps() {
		    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setMessage("Yout GPS seems to be disabled, You need GPS to run this application. do you want to enable it?")
		           .setCancelable(false)
		           .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		           		 whatsnoisy.this.startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), START_GPS);
		               }
		           })
		           .setNegativeButton("No", new DialogInterface.OnClickListener() {
		               public void onClick(final DialogInterface dialog, final int id) {
		            	   whatsnoisy.this.finish();
		               }
		           });
		    final AlertDialog alert = builder.create();
		    alert.show();
		}
	
	private void authUser() {
		//should start the create account page if user account is not linked to phone
		GoogleLoginServiceHelper.getCredentials(
				this,
				GET_ACCOUNT_REQUEST,
				null,
				GoogleLoginServiceConstants.PREFER_HOSTED,
				"ah",
				true);
	}

	private void startServices() {
		Intent service = new Intent();

		service.setClass(this, LocationService.class);
		startService(service);

		service.setClass(this, LocationTrace.class);
		startService(service);
		
		service.setClass(this, LocationTraceUpload.class);
		startService(service);
		
		service.setClass(this, SampleUpload.class);
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
	}


	private class AuthorizeTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... progress) {
			//authToken = getAuthToken();
			
			Boolean result = authorize(authToken);
			if(!result) {
				try {
					//try reseting the authtoken and trying again
					GoogleLoginServiceBlockingHelper.invalidateAuthToken(whatsnoisy.this, authToken);
				} catch (GoogleLoginServiceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				result=authorize(authToken);
			}

			return result;
		}

		protected void onPostExecute(Boolean result) {
			//save if we have been authenticated or not
			preferences.edit().putBoolean("authenticated", result).commit();

			startServices();
			
			if(result) {
				Log.d(TAG, "authorized");


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
					Intent uploadService = new Intent(whatsnoisy.this, LocationTraceUpload.class);
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


	protected boolean authorize(String authToken) {
		DefaultHttpClient httpClient = new DefaultHttpClient();


		String base_url = getString(R.string.base_url);
		HttpGet auth_request = new HttpGet(base_url + "/_ah/login?continue=" + base_url + "&auth="+authToken);
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

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == RECORD_FINISHED) {
			finish();
		}
		else if (requestCode == GET_ACCOUNT_REQUEST) {
			authToken = intent.getStringExtra(GoogleLoginServiceConstants.AUTHTOKEN_KEY);
			new AuthorizeTask().execute();
		} 
		else if (requestCode == SHOW_HELP) {
			authUser();
		} 
		else if (requestCode == START_GPS ){
			Log.d(TAG, "Started GPS?");
			if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
				buildAlertMessageNoGps();
			} else {
				authUser();
			}
		}
	}


}