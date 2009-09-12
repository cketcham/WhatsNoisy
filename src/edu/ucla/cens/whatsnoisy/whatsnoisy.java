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

import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.google.android.googlelogin.GoogleLoginServiceHelper;

import edu.ucla.cens.whatsnoisy.services.SampleUpload;
import edu.ucla.cens.whatsnoisy.tools.AudioRecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class whatsnoisy extends Activity {
	
	// An arbitrary constant to pass to the GoogleLoginHelperService
    private static final int GET_ACCOUNT_REQUEST = 1;
	private static final String TAG = null;
	private static final String PREFERENCES_USER = "user";
	SharedPreferences preferences;
    private Button Button;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        preferences = getSharedPreferences(PREFERENCES_USER, Activity.MODE_PRIVATE);
        
        if(authorize()) {
        	Log.d(TAG, "authorized");
        	Intent uploadService = new Intent(whatsnoisy.this, SampleUpload.class);
        	whatsnoisy.this.startService(uploadService);
        }
        
        Button = (Button)this.findViewById(R.id.Button01);
		Button.setOnClickListener(new View.OnClickListener() { 
			public void onClick(View v) {
				//start recording if not already recording
						Intent act = new Intent(whatsnoisy.this, Record.class);
						whatsnoisy.this.startActivity(act);
			}
		});
    }
    
    protected boolean authorize() {
    	DefaultHttpClient httpClient = new DefaultHttpClient();

    	String auth = preferences.getString("AUTHTOKEN", "");
    	if(auth=="") {
    		updateAuthToken();
    		return false;
    	}
    		
    	HttpGet auth_request = new HttpGet("http://cketcham.appspot.com/_ah/login?continue=http://cketcham.appspot.com/sample&auth="+auth);
    	HttpResponse response;
    	
    	
		try {
			response = httpClient.execute(auth_request);
			int status = response.getStatusLine().getStatusCode();
			
	    	Log.d(TAG, generateString(response.getEntity().getContent()));
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
    
    protected void updateAuthToken() {
        Bundle bundle = new Bundle();
        bundle.putCharSequence("optional_message", "no optional message?");
        GoogleLoginServiceHelper.getCredentials(
                this,
                GET_ACCOUNT_REQUEST,
                bundle,
                GoogleLoginServiceConstants.PREFER_HOSTED,
                "ah",
                true);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == GET_ACCOUNT_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (intent != null) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        final String account;
                        final String auth_token;
                        account = extras.getString(GoogleLoginServiceConstants.AUTH_ACCOUNT_KEY);
                        auth_token = extras.getString(GoogleLoginServiceConstants.AUTHTOKEN_KEY);
                        Log.d(TAG,"account: "+account);
                        Log.d(TAG,"authtoken: "+auth_token);
                        
                        Editor edit = preferences.edit();
                        edit.putString("AUTHTOKEN", auth_token);
                        edit.commit();
                        
                        authorize();
                        
                        Intent uploadService = new Intent(whatsnoisy.this, SampleUpload.class);
                    	whatsnoisy.this.startService(uploadService);

                    }
                }
            } else {
                finish();
            }
        }
    }
    
	public String generateString(InputStream stream) {
		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader buffer = new BufferedReader(reader);
		StringBuilder sb = new StringBuilder();

		try {
			String cur;
			while ((cur = buffer.readLine()) != null) {
				sb.append(cur + "\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}


	/*
	 * Read file into String. 
	 */
	private String readFileAsString(File file) throws java.io.IOException{
		StringBuilder fileData = new StringBuilder(1024);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		char[] buf = new char[1024];
		int numRead=0;
		while((numRead=reader.read(buf)) != -1){
			fileData.append(buf, 0, numRead);
		}
		reader.close();
		return fileData.toString();
	}	

    
}