package com.nus.cs4222.isbtracker;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

/**
 * Used for communications with the server.
 * @author Gabriel
 */
public class ServerSideComms {
	Context mContext;
	String host = "http://limyx.no-ip.org";
	String path = "/server/";
	int port = 8000;
	String fileName = "Timings.csv";
	
	void pushData(int bsStart, String timeStart, double waitTime) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(host + ":" + port + path + "submit.php");

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	        nameValuePairs.add(new BasicNameValuePair("bsStart", Integer.toString(bsStart)));
	        nameValuePairs.add(new BasicNameValuePair("timeStart", timeStart));
	        nameValuePairs.add(new BasicNameValuePair("timeWait", Double.toString(waitTime)));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        
	        StringBuilder inputStringBuilder = new StringBuilder();
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	        String line = bufferedReader.readLine();
	        while(line != null){
	            inputStringBuilder.append(line);inputStringBuilder.append('\n');
	            line = bufferedReader.readLine();
	        }
	        System.out.println(inputStringBuilder.toString());

	        Log.d("isbtracker.serversidecomms.push", inputStringBuilder.toString());
	        

	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    	
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
		
	}

	void getData() {
		Context context = ApplicationContext.get();
		String url = host + ":" +  port + path + "get.php";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String line = null;
			
			StringBuffer sb = new StringBuffer();
			while((line = in.readLine()) != null) {
				sb.append(line);
			}
			
			File file = new File(context.getExternalFilesDir(null), fileName);
			FileOutputStream os = null;
			try {
			    os = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
			    System.err.println("Error while creating FileOutputStream");
			    e.printStackTrace();
			}
			os.write(sb.toString().getBytes());
			os.close();
		} catch (MalformedURLException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}

	}
}
