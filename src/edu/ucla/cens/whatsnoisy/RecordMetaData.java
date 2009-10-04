package edu.ucla.cens.whatsnoisy;

import java.util.logging.Logger;

import edu.ucla.cens.whatsnoisy.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;


public class RecordMetaData extends Activity {
	protected static final String TAG = "Record Metadata";
	private Button submitButton;
	private EditText title;
	private RadioButton funButton;
	private RadioButton noisyButton;
	private String type;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_metadata);
		
		title = (EditText)this.findViewById(R.id.title);
		funButton = (RadioButton)this.findViewById(R.id.fun_button);
		noisyButton = (RadioButton)this.findViewById(R.id.noisy_button);

		submitButton = (Button)this.findViewById(R.id.submit_sample);
		submitButton.setOnClickListener(new View.OnClickListener() { 
			public void onClick(View v) {

				if(funButton.isChecked())
					type = "Fun";
				else if(noisyButton.isChecked())
					type = "Noisy";
				else {
					//TODO need to choose one
				}
				
				Intent data = new Intent(RecordMetaData.this, Record.class);
				Log.d(TAG, "title: " + title.getText().toString());
				data.putExtra("title", title.getText().toString());
				data.putExtra("type", type);
				
				RecordMetaData.this.setResult(RESULT_OK, data);
				
				RecordMetaData.this.finish();
				

			}
		});
	}
}