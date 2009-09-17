package edu.ucla.cens.whatsnoisy.services;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.params.BasicHttpParams;
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
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase.SampleRow;

public class SampleUpload extends Service{

	private SampleDatabase sdb;
	private static final String TAG = "SampleUploadThread";
	private CustomHttpClient httpClient;
	private SharedPreferences preferences;
	private PostThread post;

	@Override
	public void onCreate() {
		super.onCreate();
		
		preferences = getSharedPreferences(Settings.NAME, Activity.MODE_PRIVATE);
		
		post = new PostThread();
		
		//do not upload if we aren't authenticated, or upload is turned off
		Log.d(TAG, "toggle_audio_upload is " + preferences.getBoolean("toggle_audio_upload", false));
		if(!preferences.getBoolean("authenticated", false) || !preferences.getBoolean("toggle_audio_upload", true))
		{
			stopSelf();
		}
		
		sdb = new SampleDatabase(this);
		
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
					Log.d(TAG, "Uploading Samples");

					//list all trace files
					sdb.openRead();
					ArrayList<SampleRow>  entries = sdb.fetchAllSamples();
					sdb.close();

					Log.d(TAG, "Points to submit: " + Integer.toString(entries.size()));

					for (int i=0; i < entries.size(); i++)
					{
						SampleRow sample = entries.get(i);
						File file = null;
						if ((sample.path != null) || (sample.path.toString() != ""))
							file = new File(sample.path.toString());

						try
						{
							Log.d(TAG, "Posting file");
							if(httpClient.postFile(getString(R.string.upload_url), sample.path, sample.title, sample.type, sample.getLocation()))
							{
								if(file != null)
								{
									file.delete();
								}
								sdb.openWrite();
								sdb.deleteSample(sample.key);
								if (!sdb.hasSamples())
								{
									//no more samples so stop the uploadservice
									SampleUpload.this.stopSelf();
								}
								sdb.close();
							}
						}
						catch (Exception e) 
						{
							// TODO Auto-generated catch block
							Log.d(TAG, "threw an IOException for sending file.");
							e.printStackTrace();	
						}
						

					}
					
					// Sleeping for 5 mintutes?
					Thread.sleep(1*60000);
					
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
