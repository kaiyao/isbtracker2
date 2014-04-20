package com.nus.cs4222.isbtracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * Used for communications with the server.
 * @author Gabriel
 */
public class ServerSideComms {
	String host = "localhost";
	String script = "/server/submit.php";
	int port = 80;
	
	void pushData(int bsStart, String timeStart, int waitTime) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(host + ":" + port + script);

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	        nameValuePairs.add(new BasicNameValuePair("bsStart", Integer.toString(bsStart)));
	        nameValuePairs.add(new BasicNameValuePair("timeStart", timeStart));
	        nameValuePairs.add(new BasicNameValuePair("timeWait", Integer.toString(waitTime)));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);

	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    	
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
		
	}
}
