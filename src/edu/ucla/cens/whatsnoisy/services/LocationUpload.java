package edu.ucla.cens.whatsnoisy.services;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONStringer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import edu.ucla.cens.whatsnoisy.tools.CustomHttpClient;
import edu.ucla.cens.whatsnoisy.R;
import edu.ucla.cens.whatsnoisy.Settings;
import edu.ucla.cens.whatsnoisy.data.LocationDatabase;
import edu.ucla.cens.whatsnoisy.data.LocationDatabase.LocationRow;

public class LocationUpload extends Service{

	private LocationDatabase ldb;
	private static final String TAG = "LocationUploadThread";
	private CustomHttpClient httpClient;
	private SharedPreferences preferences;
	private PostThread post;

	@Override
	public void onCreate() {
		super.onCreate();

		preferences = getSharedPreferences(Settings.NAME, Activity.MODE_PRIVATE);

		post = new PostThread();

		//do not upload if we aren't authenticated, or upload is turned off
		if(!preferences.getBoolean("authenticated", false) || !preferences.getBoolean("toggle_location_upload", true))
		{
			stopSelf();
		}

		ldb = new LocationDatabase(this);

		httpClient = new CustomHttpClient(preferences.getString("AUTHCOOKIE", ""));

		post.start();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Stopping the thread");
		post.exit();
	}

	public class PostThread extends Thread{

		public Boolean runThread = true;

		public void run(){

			try {
				while(runThread)
				{

					Log.d(TAG, "Running the thread");
					Log.d(TAG, "Uploading Location");

					//list all trace files
					ldb.open();
					ArrayList<LocationRow>  entries = ldb.fetchAllPoints();
					ldb.close();

					Log.d(TAG, "Points to submit: " + Integer.toString(entries.size()));

					JSONStringer locjson = new JSONStringer();
					locjson.array();

					for (int i=0; i < entries.size(); i++)
					{
						LocationRow locpoint = entries.get(i);

							locjson.object();
							locjson.key("latitude");
							locjson.value(locpoint.location.getLatitude());
							locjson.key("longitude");
							locjson.value(locpoint.location.getLongitude());
							locjson.key("time");
							locjson.value(locpoint.location.getTime());
							locjson.endObject();		
					}
					
					locjson.endArray();

					String xmldata = "<table><row>";
					xmldata += "<field name=\"location\">";
					xmldata += locjson.toString();
					xmldata += "</field>";
					xmldata += "<field name=\"datetime\">";
					Date date = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");						
					xmldata += dateFormat.format(date);
					xmldata += "</field>";						
					xmldata += "</row></table>";

					
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("data_string", xmldata));
					params.add(new BasicNameValuePair("type", "xml"));




					try
					{
						Log.d(TAG, "Posting file");
						if(httpClient.doPost(getString(R.string.trace_post_url), params))
						{

							for (int i=0;i < entries.size(); i++) {
								ldb.open();
								ldb.deletePoint(entries.get(i).key);
								ldb.close();
							}
						}
					}
					catch (Exception e) 
					{
						// TODO Auto-generated catch block
						Log.d(TAG, "threw an IOException for sending file.");
						e.printStackTrace();	
					}


				}
				
				
				// Sleeping for 1 minutes?
				Thread.sleep(1*60000);


			}
			catch (InterruptedException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void exit()
		{
			runThread = false;
		}
	}
}


