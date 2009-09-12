package edu.ucla.cens.whatsnoisy.services;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.params.BasicHttpParams;

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
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase.SampleRow;

public class SampleUpload extends Service{

	private SampleDatabase sdb;
	private static final String TAG = "SampleUploadThread";
	protected static final int UPLOAD_SAMPLES = 0;
	private static final String PREFERENCES_USER = "user";
	private CustomHttpClient httpClient;
	private SharedPreferences preferences;

	@Override
	public void onCreate() {
		sdb = new SampleDatabase(this);
		
        preferences = getSharedPreferences(PREFERENCES_USER, Activity.MODE_PRIVATE);
		httpClient = new CustomHttpClient(preferences.getString("AUTHCOOKIE", ""));

		uploadSamples();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "Stopping the thread");
	}

	public void uploadSamples() {
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
					/*if(file != null)
					{
						file.delete();
					}*/
					sdb.openWrite();
					sdb.deleteSample(sample.key);
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
		
		//start upload again in one minute
		Message msg = Message.obtain(handler);
		msg.arg1 = UPLOAD_SAMPLES;
		handler.sendMessageDelayed(msg, 60000);
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			if(msg.arg1 == UPLOAD_SAMPLES){
				uploadSamples();
			}
		}
	};
}
