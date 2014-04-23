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
	String lastUpdateFileName = "LastUpdated.txt";
	
	public ServerSideComms(){
		mContext = ApplicationContext.get();
	}
	
	void pushData(int bsStart, String timeStart, double waitTime) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(host + ":" + port + path + "submit.php");

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
	        nameValuePairs.add(new BasicNameValuePair("bsid", Integer.toString(bsStart)));
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
		
		String url = host + ":" +  port + path + "get.php";
		try {
			BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
			
			File file = new File(mContext.getExternalFilesDir(null), fileName);
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(file);
				
				final byte data[] = new byte[1024];
		        int count;
		        while ((count = in.read(data, 0, 1024)) != -1) {
		        	out.write(data, 0, count);
		        }
		        out.close();
			} catch (FileNotFoundException e) {
			    System.err.println("Error while creating FileOutputStream");
			    e.printStackTrace();
			}
			
			File luf = new File(mContext.getExternalFilesDir(null), lastUpdateFileName);
			BufferedWriter bw = new BufferedWriter(new FileWriter(luf));
			bw.write(""+System.currentTimeMillis());
			bw.close();
			
		} catch (MalformedURLException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		}

	}
	
	public Date getLastUpdated(){
		Date lastUpdated = new Date(0);
		
		try {
			File luf = new File(mContext.getExternalFilesDir(null), lastUpdateFileName);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(luf));
			String line;

			line = bufferedReader.readLine();		
			while(line != null){
				lastUpdated = new Date(Long.parseLong(line));
			}
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lastUpdated;
	}
}
