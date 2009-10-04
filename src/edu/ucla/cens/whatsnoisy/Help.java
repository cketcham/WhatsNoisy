package edu.ucla.cens.whatsnoisy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Help extends Activity {

	private static final String TAG = "Help";
	static final int MAIN_HELP = 0;
	static final int TAG_HELP = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.help);		
		
		this.findViewById(R.id.LinearLayout01).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
		        Help.this.finish();
			}});
	}

}
