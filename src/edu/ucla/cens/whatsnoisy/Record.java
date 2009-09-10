package edu.ucla.cens.whatsnoisy;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.ucla.cens.whatsnoisy.R;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase;
import edu.ucla.cens.whatsnoisy.data.SampleDatabase.SampleRow;
import edu.ucla.cens.whatsnoisy.tools.AudioRecorder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Record extends Activity {
	protected static final int SAVE_SAMPLE = 0;
	private Button recordButton;
	private AudioRecorder a = null;
	private LocationManager lManager;
	private SampleDatabase sdb;
	private Boolean recording = false;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_record);

		lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		sdb = new SampleDatabase(this);
		
		recordButton = (Button)this.findViewById(R.id.start);
		recordButton.setOnClickListener(new View.OnClickListener() { 
			public void onClick(View v) {
				//start recording if not already recording
				if(!recording) {

					a = new AudioRecorder();
					try {
						a.start();
						recording = true;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					try {
						a.stop();
						recording = false;
						
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
					
					sdb.openWrite();
					sdb.insertSample(sample);
					sdb.close();

	            }
	        }
	    }
}