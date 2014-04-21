package com.nus.cs4222.isbtracker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

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
	String path = "/server/";
	int port = 80;
	
	void pushData(int bsStart, String timeStart, int waitTime) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(host + ":" + port + path + "submit.php");

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

	void getData() {
		String url = host + ":" +  port + path + "get.php";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String line = null;
			
			while((line = in.readLine()) != null) {
				String[] params = line.split(",");
				int bs = Integer.parseInt(params[0]);
				String day = params[1];
				String time = params[2];
				double timeWait = Double.parseDouble(params[3]);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
