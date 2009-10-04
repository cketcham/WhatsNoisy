package edu.ucla.cens.whatsnoisy;

import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase.SampleRow;
import edu.ucla.cens.whatsnoisy.services.SampleUpload;
import edu.ucla.cens.whatsnoisy.tools.AudioRecorder;

public class Record extends Activity {
	protected static final int SAVE_SAMPLE = 0;
	protected static final String TAG = "recording";
	
	private static final int MENU_ABOUT = 0;
	private static final int MENU_QUEUE = 1;
	private static final int MENU_HELP = 2;
	private static final int MENU_SETTINGS = 3;
	protected static final int UPDATE_TIME = 0;
	
	private Button recordButton;
	private static AudioRecorder a;
	private LocationManager lManager;
	private SampleDatabase sdb;
	
	private TextView counter;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_record);

		lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		sdb = new SampleDatabase(this);
		
		if(a == null)
			a = new AudioRecorder();
		
		counter = (TextView)this.findViewById(R.id.timer);
		
		if(a.isRecording()) {
			timerHandler.postDelayed(recordingTimer, 0);
		}
	}
	
	private void setRecordText(Button recordButton)
	{
		if(a.isRecording()) {
			recordButton.setText("Stop Recording");
		} else {
			recordButton.setText("Start Recording");
		}
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		recordButton = (Button)this.findViewById(R.id.start);

		setRecordText(recordButton);

		recordButton.setOnClickListener(new View.OnClickListener() { 
			public void onClick(View v) {
				//start recording if not already recording
				if(!a.isRecording()) {
					a = new AudioRecorder();
					try {
						startTime = SystemClock.uptimeMillis();
						
						timerHandler.postDelayed(recordingTimer, 0);
						recordButton.setText("Stop Recording");
						a.start();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						
						Intent recordMetaData = new Intent(Record.this, RecordMetaData.class);
						Record.this.startActivityForResult(recordMetaData, SAVE_SAMPLE);
						
						a.stop();
						
						counter.setText("00:00:00");
						timerHandler.removeCallbacks(recordingTimer);
						recordButton.setText("Start Recording");
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putLong("startTime", startTime);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle inState) {
		super.onSaveInstanceState(inState);
		
		startTime = inState.getLong("startTime");
	}
	

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {

		Location loc = lManager.getLastKnownLocation("gps");

		if (requestCode == SAVE_SAMPLE) {
			if (resultCode == RESULT_OK) {


				SampleRow sample = new SampleRow();
				sample.title = data.getStringExtra("title");
				sample.type = data.getStringExtra("type");
				sample.location = loc;
				sample.timestamp = new Date();
				sample.path = a.getPath();
				
				Log.d(TAG,"path = " + sample.path);

				sdb.openWrite();
				sdb.insertSample(sample);

				//after insert notify sampleupload to start
				Intent uploadService = new Intent(this, SampleUpload.class);
				startService(uploadService);

				sdb.close();

			}
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_QUEUE, 2, "Queue").setIcon(android.R.drawable.ic_menu_sort_by_size);
        menu.add(0, MENU_HELP, 1, "Help").setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 3, "Settings").setIcon(android.R.drawable.ic_menu_preferences);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent= null;

        switch(item.getItemId()){
        case MENU_ABOUT:
            //intent = new Intent(this, About.class);
            //this.startActivity(intent);

            break;
        case MENU_SETTINGS:
            intent = new Intent(this, Settings.class);
            this.startActivity(intent);

            break;
        case MENU_HELP:
        	intent = new Intent(this, Help.class);
            this.startActivity(intent);
            break;
        }

        return super.onOptionsItemSelected(item);
    }
    
	private final Handler timerHandler = new Handler();
	
	private long startTime;
	
	/**
	 * Runnable to handle timer ticker updates
	 * */
	private Runnable recordingTimer = new Runnable() {
	   public void run() {
	       final long start = startTime;
	       long millis = SystemClock.uptimeMillis() - start;
	       int secondsElapsed = (int) (millis / 1000);
	       int seconds = (int) (millis / 1000);
	       int minutes = (int) Math.floor(seconds/60.0);
	       int hours = (int) Math.floor(minutes/60.0);
	       seconds = seconds % 60;
	       counter.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
	       timerHandler.postAtTime(this, start + (secondsElapsed + 1) * 1000);
	   }
	};
}