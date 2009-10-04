package edu.ucla.cens.whatsnoisy.services;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import edu.ucla.cens.whatsnoisy.tools.PolylineEncoder;
import edu.ucla.cens.whatsnoisy.tools.Track;
import edu.ucla.cens.whatsnoisy.R;
import edu.ucla.cens.whatsnoisy.Settings;
import edu.ucla.cens.whatsnoisy.data.LocationDatabase;
import edu.ucla.cens.whatsnoisy.data.LocationDatabase.LocationRow;

public class LocationTraceUpload extends Service{

	private LocationDatabase ldb;
	private static final String TAG = "LocationTraceUploadThread";
	private CustomHttpClient httpClient;
	private SharedPreferences preferences;
	private PostThread post;

	@Override
	public void onCreate() {
		super.onCreate();

		preferences = getSharedPreferences(Settings.NAME, Activity.MODE_PRIVATE);

		//do not upload if we aren't authenticated, or upload is turned off
		if(!preferences.getBoolean("authenticated", false) || !preferences.getBoolean("location_trace_upload", false))
		{
			stopSelf();
		} else {

			post = new PostThread();

			ldb = new LocationDatabase(this);

			httpClient = new CustomHttpClient(preferences.getString("AUTHCOOKIE", ""));

			post.start();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Stopping the thread");
		if(post!=null)
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

					PolylineEncoder pe = new PolylineEncoder();
					HashMap<String, String> encoded = pe.dpEncode(new Track(entries));


					Log.d(TAG, "Points to submit: " + Integer.toString(entries.size()));

					if(entries.size() != 0) {





						Log.d(TAG,encoded.toString());

						Iterator<String> iter = encoded.values().iterator();

						List<NameValuePair> params = new ArrayList<NameValuePair>();
						params.add(new BasicNameValuePair("encodedPoints", iter.next().toString()));
						params.add(new BasicNameValuePair("encodedLevels", iter.next().toString()));
						params.add(new BasicNameValuePair("zoomFactor", Integer.toString(pe.getZoomFactor())));
						params.add(new BasicNameValuePair("numLevels", Integer.toString(pe.getNumLevels())));


						try
						{
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
							Log.d(TAG, "threw an IOException for sending data.");
							e.printStackTrace();	
						}

					}
					//LocationTraceUpload.this.stopSelf();
					// Sleeping for some minutes
					Thread.sleep(new Long(preferences.getString("location_upload_frequency", "5"))*60000);
				}
			}
			catch (InterruptedException e) 
			{
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


