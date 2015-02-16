package com.mto.android.app.idss.thread;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.mto.android.app.idss.activity.MainActivity;
import com.mto.android.app.idss.structure.FixedSizeLinkedQueue;
import com.mto.android.app.idss.util.Utils;

public class ServerAsyncTask extends AsyncTask<String, Void, String>{
	//URl to connect to the servlet to send and receive location data	
	private static final String URL = "xxxx";//URI is only for UMN staff and studentsd

	String output = "Connection failed";
	String outputPost="Connection failed post";
	FixedSizeLinkedQueue mLocationList;
	Context mContext;
	String mDate;

	public ServerAsyncTask(FixedSizeLinkedQueue locationList,
			Context context, String date) {

		if(mLocationList != null){
			mLocationList.clear();
		}
		mLocationList = locationList;
		mContext = context;
		mDate = date;

	}

	@Override
	protected String doInBackground(String... params) {

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(URL);
		HttpPost httppost = new HttpPost(URL);
		try {
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			output = EntityUtils.toString(httpEntity);
			Calendar c = Calendar.getInstance();
			int min = c.get(Calendar.MINUTE);
			int sec = c.get(Calendar.SECOND);

			if(sec/10 < 1){
				mDate = Utils.getCalendar(Calendar.MONTH)+1 +"/" +Utils.getCalendar(Calendar.DAY_OF_MONTH) +"/" +Utils.getCalendar(Calendar.YEAR) +" " +Utils.getCalendar(Calendar.HOUR_OF_DAY)+":" +min +":0" +sec;
			}else
				mDate = Utils.getCalendar(Calendar.MONTH)+1 +"/" +Utils.getCalendar(Calendar.DAY_OF_MONTH) +"/" +Utils.getCalendar(Calendar.YEAR) +" " +Utils.getCalendar(Calendar.HOUR_OF_DAY)+":" +min +":"  +sec;
			long date = Date.parse(mDate);
			String format = "yyyy-MM-dd HH:mm:ss";
			SimpleDateFormat sdf= new SimpleDateFormat(format,Locale.US);


			ArrayList<NameValuePair> namevaluepairs = new ArrayList<NameValuePair>(6);

			namevaluepairs.add(new BasicNameValuePair("date", sdf.format(date)));
			if(null != mLocationList){
				namevaluepairs.add(new BasicNameValuePair("curLon", Double.toString(mLocationList.getLatestValue().longitude)));
				namevaluepairs.add(new BasicNameValuePair("curLat", Double.toString(mLocationList.getLatestValue().latitude)));
				namevaluepairs.add(new BasicNameValuePair("prevLon", Double.toString(mLocationList.getTenthlastvalue().longitude)));
				namevaluepairs.add(new BasicNameValuePair("prevLat", Double.toString(mLocationList.getTenthlastvalue().latitude)));
			}else{
				namevaluepairs.add(new BasicNameValuePair("curLon", ""));
				namevaluepairs.add(new BasicNameValuePair("curLat", ""));
				namevaluepairs.add(new BasicNameValuePair("prevLon", ""));
				namevaluepairs.add(new BasicNameValuePair("prevLat", ""));
			}
			namevaluepairs.add(new BasicNameValuePair("look_ahead", ""+Utils.getLookAheadDistance()));	 

			httppost.setEntity(new UrlEncodedFormEntity(namevaluepairs));
			HttpResponse httpresponsePost = httpClient.execute(httppost);
			HttpEntity httpEntityPost = httpresponsePost.getEntity();
			outputPost = EntityUtils.toString(httpEntityPost);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);


		Toast.makeText(mContext, output, Toast.LENGTH_SHORT).show();
		Toast.makeText(mContext, outputPost, Toast.LENGTH_SHORT).show();
		if(outputPost!= null && !outputPost.equals(" ")&& outputPost.lastIndexOf(']') >outputPost.indexOf('[')){
			String subOutputPost = outputPost.substring(outputPost.indexOf('[')+1, outputPost.lastIndexOf(']'));
			String[] str = subOutputPost.split(", ");
			for(String s: str){	
				if(s.indexOf("Latitude")>0){
					try{
						String incident_segment = s.substring(s.indexOf("Segment id: ")+12, s.indexOf("Longitude")-1);
						String longitude = s.substring(s.indexOf("Longitude: ")+11, s.indexOf("Latitude"));
						String latitude =  s.substring(s.indexOf("Latitude: ")+10, s.indexOf("timestamp"));
						String timeStamp = s.substring(s.indexOf("timestamp: ")+11, s.indexOf("route_name"));
						String curr_route = s.substring(s.indexOf("route_name: ")+12, s.indexOf("previous_segment_id")-1);
						String  curr_segment = s.substring(s.indexOf("previous_segment_id: ")+21, s.indexOf("previous_route_name")-1);
						String incident_route = s.substring(s.indexOf("previous_route_name: ")+21, s.indexOf("distance")-1);
						String eucl_distance = s.substring(s.indexOf("distance: ") +10);


						Log.d("sandeep","string; " +s);
						Log.d("sandeep", "curr_segment: " +curr_segment);
						Log.d("sandeep", "longitude: " +longitude);
						Log.d("sandeep", "latitude: " +latitude);
						Log.d("sandeep", "timestamp; " +timeStamp);
						Log.d("sandeep", "current_route: " +curr_route);
						Log.d("sandeep", "incident_segment: " +incident_segment);
						Log.d("sandeep", "incident_route: " +incident_route);
						Log.d("sandeep", "distance: " +eucl_distance);

						if(MainActivity.getInstance()!=null){
							MainActivity.getInstance().addMapMarker(Double.parseDouble(latitude), Double.parseDouble(longitude), 
									BitmapDescriptorFactory.HUE_RED, "Accident Information", "Please take care");
							float[] distance = new float[2];
							Location.distanceBetween( Double.parseDouble(latitude),Double.parseDouble(longitude),
									mLocationList.getLatestValue().latitude, mLocationList.getLatestValue().longitude, distance);
							String time_diff = gettimediff(mDate,timeStamp);
							String distanceFromIncident = getDistanceFromIncident(Integer.parseInt(curr_segment), Integer.parseInt(incident_segment), 
									curr_route, incident_route,
									Double.parseDouble(latitude),Double.parseDouble(longitude),
									mLocationList.getLatestValue().latitude, mLocationList.getLatestValue().longitude, eucl_distance );

							DecimalFormat df = new DecimalFormat();
							df.setMaximumFractionDigits(2);

							if(null != MainActivity.getInstance().getTTSengine() && !MainActivity.getInstance().getTTSengine().isSpeaking() && Utils.isTTSactivated())
								MainActivity.getInstance().getTTSengine().speak("Attention! Incident occurred " +distanceFromIncident +" ahead " +time_diff +" ago ",
										TextToSpeech.QUEUE_FLUSH, null);
						}



					}catch (ArrayIndexOutOfBoundsException e ){
						e.printStackTrace();
						continue;
					}
				}else{
					if(null != MainActivity.getInstance()){
						MainActivity.getInstance().clearMarkerList();
					}
				}

			}
		}
	}
	private String getDistanceFromIncident(int curr_segment, int incident_segment,
			String curr_route, String incident_route, double incident_latitude,
			double incident_logitude, double latitude, double longitude, String eu_distance) {
		String distanceInString=null;
		if(curr_route.equals(incident_route)){
			if(curr_segment == incident_segment)
				distanceInString = " on " +incident_route;
			else{
				int segment_diff= (Math.abs(incident_segment - curr_segment)) ;
				if(segment_diff==1)
					distanceInString = " on " +incident_route +"," +segment_diff + "mile";
				else
					distanceInString = " on " +incident_route +"," +segment_diff +"miles";
			}

		}else{
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			/*float[] distance = new float[2];
			Location.distanceBetween(incident_latitude,incident_logitude,
					latitude, longitude, distance);

			double distanceInMiles = distance[0]/1609.34;*/
			if(Double.parseDouble(eu_distance) <=1 )
				distanceInString = " on " +incident_route +",";
			else
				distanceInString = " on " +incident_route +"," +df.format(Double.parseDouble(eu_distance))+"miles";
		}

		return distanceInString;
	}

	private String gettimediff(String mDate2, String timeStamp) {

		String format = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat sdf= new SimpleDateFormat(format,Locale.US);		
		long date = Date.parse(mDate);

		long duration =0;
		mDate2 = mDate2.replace('/', '-');
		Date startDate ;
		Date endDate;
		try {
			mDate2 = sdf.format(date);
			startDate = sdf.parse(timeStamp);
			endDate = sdf.parse(mDate2);
			duration = endDate.getTime() - startDate.getTime();
		} catch (java.text.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);

		diffInMinutes =  (diffInMinutes + 4) / 5 * 5; //rounding off to nearest 5 multiple
		String returnDiff = "zero minutes";
		if(diffInHours>1){
			if(diffInMinutes>10){
				returnDiff = " " +diffInHours +"hours" +diffInMinutes  +"minutes";
			}else
				returnDiff = " " +diffInHours +"hours";
		}else if(diffInHours==1){
			if(diffInMinutes>10){
				returnDiff = " " +diffInHours +"hour" +diffInMinutes  +"minutes";
			}else
				returnDiff = " " +diffInHours +"hour";
		}else if(diffInHours==0){
			if(diffInMinutes>10){
				returnDiff = diffInMinutes  +"minutes";
			}else
				returnDiff = "few minutes";
		}

		return returnDiff;
	}
}	
