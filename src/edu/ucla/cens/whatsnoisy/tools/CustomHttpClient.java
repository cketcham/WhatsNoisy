package edu.ucla.cens.whatsnoisy.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import edu.ucla.cens.whatsnoisy.whatsnoisy;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

public class CustomHttpClient extends DefaultHttpClient {
	private final int CONNECT_TIMEOUT = 10000;
	private final int SOCKET_TIMEOUT = 10000;
	private static final String TAG = "HttpClient";
	private final String authtoken;

	public CustomHttpClient(String authtoken){
		HttpParams httpParams = this.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, SOCKET_TIMEOUT);

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

//		[version: 0][name: ACSID][value: AJKiYcHjxH9f5Zfcvi00CyOrOperWEGCmULZAuDacjwHjs_q5McMEthUeGbJ6kOK1rSMOsS9mzEvkUsoejWR_KMdLZA_w1hJIGppvRK1I8XCpKxPw4obL0ibVBBH0AlvFj93AYQ0uTL_W1uDzkWL9_lwUOlIXOaB71nRLP07diL3sxqOrGD_6yCVaSdrmvq1WrnkP4lBSC5KSJVIV4GvjCB7dp0YhtG_ZOl0CYaHj4BQHzVFbCSHWKGNXEDUQl4awVMp8Zy0uL1q0Z1qA08JDOYMbDsrWL523Tv51AxNYIFF1p8OgUqEKcoXorVTs2r3eVvO9OKtqAi7fZf_5S7YTcQbih7HCb_4c2y1hrC0PMypotmC0FXUu-AIc9s50YGFnoRonLLxkL7bLnehsHOy0XD8qiDSDZeht8uTx21xFu-tVIHwLs5ZxWj67VqW0CgXotC7NW8q3f2d][domain: cketcham.appspot.com][path: /][expiry: Sat Sep 12 14:58:01 PDT 2009]

	public boolean postFile(String url, String filename, String title, String type, String location) throws Exception {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		BasicClientCookie cookie = new BasicClientCookie("ACSID",authtoken);
		cookie.setVersion(0);
		cookie.setDomain("cketcham.appspot.com");
		cookie.setPath("/");

		CookieStore cs = httpClient.getCookieStore();
		cs.addCookie(cookie);

		for(int i=0;i<httpClient.getCookieStore().getCookies().toArray().length;i++)
		Log.d(TAG, "cookies?=" + httpClient.getCookieStore().getCookies().get(i));
		Log.d(TAG, "httpClient... " + httpClient);
		HttpPost request = new HttpPost(url.toString());

		Log.d(TAG, "posting url: " + url);
		
		//equest.addHeader("Authorization", "GoogleLogin auth="+authtoken);
		//Header[] headers = request.getAllHeaders();
		//for(int i=0;i<headers.length;i++)
		//	Log.d(TAG, "header="+headers[i].toString());
		Log.d(TAG, "After Request");

		MultipartEntity entity = new MultipartEntity();
		entity.addPart("title", new StringBody(title.toString()));
		entity.addPart("type", new StringBody(type.toString()));
		entity.addPart("location", new StringBody(location.toString()));


		Log.d(TAG, "After adding string");

		File file = new File(filename.toString());
		FileBody fileB = new FileBody(file);	    	
		entity.addPart("file", fileB);

		Log.d(TAG, "After adding file");

		request.setEntity(entity);

		Log.d(TAG, "After setting entity");

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
}
