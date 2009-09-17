package edu.ucla.cens.whatsnoisy;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucla.cens.whatsnoisy.whatsnoisy;
import edu.ucla.cens.whatsnoisy.R;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase.SampleRow;
import edu.ucla.cens.whatsnoisy.services.SampleUpload;
import edu.ucla.cens.whatsnoisy.tools.AudioRecorder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class Record extends Activity {
	protected static final int SAVE_SAMPLE = 0;
	protected static final String TAG = "recording";
	
	private static final int MENU_ABOUT = 0;
	private static final int MENU_QUEUE = 1;
	private static final int MENU_HELP = 2;
	private static final int MENU_SETTINGS = 3;
	
	private Button recordButton;
	private static AudioRecorder a;
	private LocationManager lManager;
	private SampleDatabase sdb;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_record);

		lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		sdb = new SampleDatabase(this);
		
		if(a == null)
			a = new AudioRecorder();
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
						recordButton.setText("Stop Recording");
						a.start();
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						
						a.stop();
						recordButton.setText("Start Recording");

						Intent recordMetaData = new Intent(Record.this, RecordMetaData.class);
						Record.this.startActivityForResult(recordMetaData, SAVE_SAMPLE);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
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
				sample.datetime = new Date();
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
            // TODO: Open view specific dialog
            break;
        }

        return super.onOptionsItemSelected(item);
    }
}