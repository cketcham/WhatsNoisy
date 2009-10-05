package edu.ucla.cens.whatsnoisy.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class CustomHttpClient extends DefaultHttpClient {
	private static final String TAG = "HttpClient";
	private final String authtoken;

	public CustomHttpClient(String authtoken){
		this.authtoken = authtoken;
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


	public boolean postFile(String url, String filename, String title, String type, String location, Date timestamp) throws Exception {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		BasicClientCookie cookie = new BasicClientCookie("ACSID",authtoken);
		cookie.setVersion(0);
		String domain = url.split("/")[2];

		cookie.setDomain(domain);
		cookie.setPath("/");

		CookieStore cs = httpClient.getCookieStore();
		cs.addCookie(cookie);
		
		HttpPost request = new HttpPost(url.toString());

		Log.d(TAG, "posting url: " + url);

		MultipartEntity entity = new MultipartEntity();
		entity.addPart("title", new StringBody(title.toString()));
		entity.addPart("type", new StringBody(type.toString()));
		entity.addPart("location", new StringBody(location.toString()));
		entity.addPart("timestamp", new StringBody(Long.toString(timestamp.getTime())));

		File file = new File(filename.toString());
		FileBody fileB = new FileBody(file);	    	
		entity.addPart("file", fileB);
		
		request.setEntity(entity);
		
		HttpResponse response = httpClient.execute(request);

		Log.d(TAG, "Doing HTTP Reqest");

		int status = response.getStatusLine().getStatusCode();
			Log.d(TAG, generateString(response.getEntity().getContent()));
		Log.d(TAG, "Status Message: "+Integer.toString(status));

		if(status == HttpStatus.SC_OK)
		{
			Log.d(TAG, "Sent file.");
			return true;
		}
		else
		{
			Log.d(TAG, "File not sent.");
			return false;
		}


	}
	
	/*
	 * this uses java.net.HttpURLConnection another way to do it is to use apache HttpPost
	 * but the API seems a bit complicated. If you figure out how to use it and its more
	 * efficient then let me know (vids@ucla.edu) Thanks.
	 */
	public boolean doPost(String url, List<NameValuePair> params) throws IOException 
	{
	DefaultHttpClient httpClient = new DefaultHttpClient();
		
		BasicClientCookie cookie = new BasicClientCookie("ACSID",authtoken);
		cookie.setVersion(0);
		String domain = url.split("/")[2];

		cookie.setDomain(domain);
		cookie.setPath("/");

		CookieStore cs = httpClient.getCookieStore();
		cs.addCookie(cookie);
		
		HttpPost request = new HttpPost(url.toString());

		request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

		HttpResponse response = httpClient.execute(request);

		Log.d(TAG, "Doing HTTP Reqest");

		int status = response.getStatusLine().getStatusCode();
		Log.d(TAG, "Status Message: "+Integer.toString(status));

		if(status == HttpStatus.SC_OK)
		{
			Log.d(TAG, "Sent data.");
			return true;
		}
		else
		{
			Log.d(TAG, "Data not sent.");
			return false;
		}

	}
}
