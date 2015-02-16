package com.mto.android.app.idss.activity;



import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mto.android.app.idss.R;
import com.mto.android.app.idss.structure.FixedSizeLinkedQueue;
import com.mto.android.app.idss.structure.Node;
import com.mto.android.app.idss.thread.ServerAsyncTask;
import com.mto.android.app.idss.util.Utils;


public class MainActivity extends FragmentActivity implements OnMapClickListener, OnMapLongClickListener, OnMarkerClickListener, 
OnInfoWindowClickListener, OnInitListener, LocationListener,  GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener
{
	private static MainActivity _instance;

	// Update interval in milliseconds for location services
	private static final long UPDATE_INTERVAL = 5000;
	// Fastest update interval in milliseconds for location services
	private static final long FASTEST_INTERVAL = 1000;	
	// Google Play diagnostics constant
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;	
	// Speed threshold for orienting map in direction of motion (m/s) 
	private static final double SPEED_THRESH = 1;
	private static final int MAX_ZOOM = 16;

	private static final String TAG = "Mapper";
	private LocationClient locationClient;
	private Location currentLocation;
	private double currentLat;
	private double currentLon;
	private GoogleMap map;
	private LatLng map_center;
	private int zoomOffset = 5;
	private float currentZoom;
	private float bearing;
	private float speed;
	private float acc;
	private Circle localCircle;
	private TextToSpeech mTts;
	private FixedSizeLinkedQueue mLocationList;
	LatLng currentLatLng;
	private ArrayList<Marker> currentMarkerList = new ArrayList<>();		

	private double lon;
	private double lat;
	static final int numberOptions = 10;
	String [] optionArray = new String[numberOptions];	
	private Handler mHandler;
	private volatile int mInterval = Utils.getDelaySpeechinSecs();
	volatile boolean keepRunningThread = false;


	LocationRequest locationRequest;
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		map = ((MapFragment) getFragmentManager()
				.findFragmentById(R.id.mapme_map)).getMap();

		if(map != null){
			currentZoom = map.getMaxZoomLevel()-zoomOffset;
			map.setOnMapClickListener(this);
			map.setOnMapLongClickListener(this);
			map.setOnMarkerClickListener(this);
			map.setOnInfoWindowClickListener(this);

		} else {
			Toast.makeText(this, getString(R.string.nomap_error), 
					Toast.LENGTH_LONG).show();
		}
		locationClient = new LocationClient(this, this, this);
		locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(UPDATE_INTERVAL);
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		prefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		prefsEditor = prefs.edit();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		_instance = this;
		createTTSengine();
		mHandler = new Handler();
		//currentLatLng = new LatLng(0, 0);
	}


	public ArrayList<Marker> getMarkerList(){
		return currentMarkerList;
	}

	public void clearMarkerList(){
		for(Marker m: currentMarkerList){
			if(null != m)
				m.remove();
		}
		currentMarkerList.clear();
	}

	private void createTTSengine() {
		mTts = new TextToSpeech(this, this);
	}

	public TextToSpeech getTTSengine(){
		return mTts;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mapme_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SettingsDialogFragment dialog;
		if(map == null) {
			Toast.makeText(this, getString(R.string.nomap_error), 
					Toast.LENGTH_LONG).show();
			return false;
		}


		switch (item.getItemId()) {


		case R.id.starttracking:
			if(!keepRunningThread){
				Log.d("sandeep", "inside starttracking");
				keepRunningThread = true;
				(new Thread(upDateLocation)).start();
			}
			return true;

		case R.id.stoptracking:
			if(mTts == null){
				createTTSengine();
			}
			keepRunningThread = false;
			mHandler.removeCallbacks(upDateLocation);
			return true;


		case R.id.choose_date:

			dialog = new SettingsDialogFragment(R.layout.choose_date);
			dialog.show(getFragmentManager(), "SettingsDialogFragment");
			return true;
		case R.id.choose_time:
			dialog = new SettingsDialogFragment(R.layout.chhose_time);
			dialog.show(getFragmentManager(), "SettingsDialogFragment");
			return true;

		case R.id.other_settings:
			dialog = new SettingsDialogFragment(R.layout.other_settings);
			dialog.show(getFragmentManager(), "SettingsDialogFragment");
			return true;

		case R.id.reset_setiings:
			Utils.resetSettings();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return false;
	}



	@Override
	protected void onPause() {
		mTts =null;

		if(map != null){
			currentZoom = map.getCameraPosition().zoom;
			prefsEditor.putFloat("KEY_ZOOM",currentZoom);
			prefsEditor.commit();  
		}
		super.onPause();
		Log.i(TAG,"onPause: Zoom="+currentZoom);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (prefs.contains("KEY_ZOOM") && map != null){
			currentZoom = prefs.getFloat("KEY_ZOOM", MAX_ZOOM);
			if(currentZoom>MAX_ZOOM)
				currentZoom = MAX_ZOOM;
		}else			

			Log.i(TAG,"onResume: Zoom="+currentZoom);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		createTTSengine();
	}

	/* The following two lifecycle methods conserve resources by ensuring that
	 * location services are connected when the map is visible and disconnected when
	 * it is not.
	 */

	@Override
	protected void onStart() {
		super.onStart();
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		boolean isGPS = locationManager.isProviderEnabled (LocationManager.GPS_PROVIDER);
		if(!isGPS)
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
		else
			locationClient.connect();
	}

	// Called by system when Activity is no longer visible, so disconnect location
	// client, which invalidates it.

	@Override
	protected void onStop() {

		// If the client is connected, remove location updates and disconnect
		if (locationClient.isConnected()) {
			locationClient.removeLocationUpdates(this);
		}
		locationClient.disconnect();

		// Turn off the screen-always-on request
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onStop();
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		Toast.makeText(this, getString(R.string.connected_toast), 
				Toast.LENGTH_SHORT).show();

		currentLocation = locationClient.getLastLocation();
		if(currentLocation != null){
			currentLat = currentLocation.getLatitude();
			currentLon = currentLocation.getLongitude();

			map_center = new LatLng(currentLat,currentLon);
		}
		if(map != null) {
			initializeMap();
		} else {
			Toast.makeText(this, getString(R.string.nomap_error), 
					Toast.LENGTH_LONG).show();
		}

		locationClient.requestLocationUpdates(locationRequest, this);
	}



	@Override
	public void onDisconnected() {
		Toast.makeText(this, getString(R.string.disconnected_toast),
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

		if (connectionResult.hasResolution()) {
			try {				
				connectionResult.startResolutionForResult(
						this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

				
			} catch (IntentSender.SendIntentException e) {
				e.printStackTrace();
			}
		} else {
			
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	public void showErrorDialog(int errorCode){
		Log.e(TAG, "Error_Code ="+errorCode);

	}



	public static MainActivity getInstance(){
		return _instance;
	}
	private void initializeMap(){
		map.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		map.setBuildingsEnabled(false);
		map.setIndoorEnabled(false);
		map.setTrafficEnabled(false);
		map.getUiSettings().setRotateGesturesEnabled(false);
		if(null != map_center)
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(map_center,MAX_ZOOM));
	}


	public void addMapMarker (double lat, double lon, float markerColor,
			String title, String snippet){

		if(map != null){
			Marker marker = map.addMarker(new MarkerOptions()
			.title(title)
			.snippet(snippet)
			.position(new LatLng(lat,lon))
			.icon(BitmapDescriptorFactory.defaultMarker(markerColor))
					);
			marker.setDraggable(false);
			marker.showInfoWindow();
			Log.d("sandeep", "Marker added");
			currentMarkerList.add(marker);
		} else {
			Toast.makeText(this, getString(R.string.nomap_error), 
					Toast.LENGTH_LONG).show();
		}

	}

	// Decimal output formatting class that uses Java DecimalFormat. See
	// http://developer.android.com/reference/java/text/DecimalFormat.html.
	// The string "formatPattern" specifies the output formatting pattern for
	// the float or double. For example, 35.8577877288 will be returned 
	// as the string "35.85779" if formatPattern = "0.00000", and as
	// the string "3.586E01" if formatPattern = "0.000E00".

	public String formatDecimal(double number, String formatPattern){

		DecimalFormat df = new DecimalFormat(formatPattern);

		// The method format(number) with a single argument is inherited by 
		// DecimalFormat from NumberFormat.

		return df.format(number);

	}

	/* Method to change properties of camera. If your GoogleMaps instance is called map, 
	 * you can use 
	 * 
	 * map.getCameraPosition().target
	 * map.getCameraPosition().zoom
	 * map.getCameraPosition().bearing
	 * map.getCameraPosition().tilt
	 * 
	 * to get the current values of the camera position (target, which is a LatLng), 
	 * zoom, bearing, and tilt, respectively.  This permits changing only a subset of
	 * the camera properties by passing the current values for all arguments you do not
	 * wish to change.
	 * 
	 * */

	private void changeCamera(GoogleMap map, LatLng center, float zoom, 
			float bearing, float tilt, boolean animate) {

		CameraPosition cameraPosition = new CameraPosition.Builder()
		.target(center)         // Sets the center of the map
		.zoom(zoom)             // Sets the zoom
		.bearing(bearing)       // Sets the bearing of the camera 
		.tilt(tilt)             // Sets the tilt of the camera relative to nadir
		.build();               // Creates a CameraPosition from the builder

		// Move (if variable animate is false) or animate (if animate is true) to new 
		// camera properties. 

		if(animate){
			map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		} else {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		}
	}



	@Override
	public void onLocationChanged(Location newLocation) {

		bearing = newLocation.getBearing();
		speed = newLocation.getSpeed();
		acc = newLocation.getAccuracy();


		// Get latitude and longitude of updated location	
		double lat = newLocation.getLatitude();
		double lon = newLocation.getLongitude();
		currentLatLng = new LatLng(lat, lon);
		currentLocation = newLocation;
		currentLat = lat;
		currentLon = lon;


		Bundle locationExtras = newLocation.getExtras();
		// If there is no satellite info, return -1 for number of satellites
		int numberSatellites = -1;
		if(locationExtras != null){
			Log.i(TAG, "Extras:"+locationExtras.toString());
			if(locationExtras.containsKey("satellites")){
				numberSatellites = locationExtras.getInt("satellites");
			}
		}

		// Log some basic location information
		Log.i(TAG,"Lat="+formatDecimal(lat,"0.00000")
				+" Lon="+formatDecimal(lon,"0.00000")
				+" Bearing="+formatDecimal(bearing, "0.0")
				+" deg Speed="+formatDecimal(speed, "0.0")+" m/s"
				+" Accuracy="+formatDecimal(acc, "0.0")+" m"
				+" Sats="+numberSatellites);

		if(map != null) {

			// Animate camera to reflect location update. Orient the view in the
			// direction of motion, but only if the velocity is above a threshold
			// to prevent random rotations of view when velocity direction is
			// not well defined.  

			if(speed < SPEED_THRESH) {

				// Smoothly move the camera view to center on the updated location
				// without changing bearing, tilt, or zoom of camera.

				map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng));

			} else {

				// Animate motion to the updated position and also orient camera in 
				// direction of motion using current bearing, keeping the same tilt 
				// and zoom.  Note: bearing is the horizontal direction of travel 
				// for the device; it has nothing to do with orientation of the device.

				changeCamera(map, currentLatLng, currentZoom,
						bearing, map.getCameraPosition().tilt, true);
			}


		} else {
			Toast.makeText(this, getString(R.string.nomap_error), 
					Toast.LENGTH_LONG).show();
		}

		/*Node n = new Node(currentLatLng);
		if(null== mLocationList)
			mLocationList = new FixedSizeLinkedQueue(n);
		else if(Math.abs(mLocationList.getLatestValue().latitude-lat)> 0.00001 
				|| Math.abs(mLocationList.getLatestValue().longitude-lon) > 0.00001){
			mLocationList.addToList(n);

			ServerAsyncTask mytask= new ServerAsyncTask(mLocationList, getApplicationContext());		
			mytask.execute(" ");
		}*/



	}


	// Method to reverse-geocode location passed as latitude and longitude. It returns a string which
	// is the first reverse-geocode location in the returned list.  (The full list is output to the
	// logcat stream.) This method returns null if no geocoder backend is available.

	private String reverseGeocodeLocation(double latitude, double longitude){

		// Use to suppress country in returned address for brevity
		boolean omitCountry = true;

		// String to hold single address that will be returned
		String returnString = "";

		Geocoder gcoder = new Geocoder(this);

		// Note that the Geocoder uses synchronous network access, so in a serious application
		// it would be best to put it on a background thread to prevent blocking the main UI if network
		// access is slow. Here we are just giving an example of how to use it so, for simplicity, we
		// don't put it on a separate thread.  See the class RouteMapper in this package for an example
		// of making a network access on a background thread. Geocoding is implemented by a backend
		// that is not part of the core Android framework, so it is not guaranteed to be present on
		// every device.  Thus we use the static method Geocoder.isPresent() to test for presence of 
		// the required backend on the given platform.

		try{
			List<Address> results = null;
			if(Geocoder.isPresent()){
				results = gcoder.getFromLocation(latitude, longitude, numberOptions);
			} else {
				Log.i(TAG,"No geocoder accessible on this device");
				return null;
			}
			Iterator<Address> locations = results.iterator();
			String raw = "\nRaw String:\n";
			String country;
			int opCount = 0;
			while(locations.hasNext()){
				Address location = locations.next();
				if(opCount==0 && location != null){
					lat = location.getLatitude();
					lon = location.getLongitude();
				}
				country = location.getCountryName();
				if(country == null) {
					country = "";
				} else {
					country =  ", "+country;
				}
				raw += location+"\n";
				optionArray[opCount] = location.getAddressLine(0)+", "
						+location.getAddressLine(1)+country+"\n";
				if(opCount == 0){
					if(omitCountry){
						returnString = location.getAddressLine(0)+", "
								+location.getAddressLine(1)+"\n";
					} else {
						returnString = optionArray[opCount];
					}
				}
				opCount ++;
			}
			Log.i(TAG, raw);
			Log.i(TAG,"\nOptions:\n");
			for(int i=0; i<opCount; i++){
				Log.i(TAG,"("+(i+1)+") "+optionArray[i]);
			}
			Log.i(TAG,"lat="+lat+" lon="+lon);

			//Toast.makeText(this, optionArray[0], Toast.LENGTH_LONG).show();

		} catch (IOException e){
			Log.e(TAG, "I/O Failure",e);
		}

		// Return the first location entry in the list.  A more sophisticated implementation 
		// would present all location entries in optionArray to the user for choice when more 
		// than one is returned by the geodecoder.

		return returnString;	

	}


	// Callback that fires when map is tapped, passing in the latitude
	// and longitude coordinates of the tap (actually the point on the ground 
	// projected from the screen tap).  This will be invoked only if no overlays 
	// on the map intercept the click first.  Here we will just issue a Toast
	// displaying the map coordinates that were tapped.  See the onMapLongClick
	// handler for an example of additional actions that could be taken.

	@Override
	public void onMapClick(LatLng latlng) {

		String f = "0.0000";
		double lat = latlng.latitude;
		double lon = latlng.longitude;
		Toast.makeText(this, "Latitude="+formatDecimal(lat,f)+" Longitude="
				+formatDecimal(lon,f), Toast.LENGTH_LONG).show();

		addMapMarker(lat, lon, BitmapDescriptorFactory.HUE_GREEN, "", "");

		if(null==map)
			return;
		Location location = map.getMyLocation();
		float[] distance = new float[2];

		if(null == location)
			return;
		Location.distanceBetween( latlng.latitude, latlng.longitude,
				location.getLatitude(), location.getLongitude(), distance);

		if( distance[0] > acc ){
			Toast.makeText(getBaseContext(), "Outside", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getBaseContext(), "Inside", Toast.LENGTH_LONG).show();
		}

	}

	// This callback fires for long clicks on the map, passing in the LatLng coordinates

	@Override
	public void onMapLongClick(LatLng latlng) {

		double lat = latlng.latitude;
		double lon = latlng.longitude;

		String title = reverseGeocodeLocation(latlng.latitude, latlng.longitude);

		String snippet="Tap marker to delete; tap window for Street View";

		// Add an orange marker on map at position of tap (default marker color is red).
		addMapMarker(lat, lon, BitmapDescriptorFactory.HUE_ORANGE, title, snippet);

		// Add a circle centered on the marker given the current position uncertainty
		// Keep a reference to the returned circle so we can remove it later.

		localCircle = addCircle (lat, lon, acc, "#ff000000", "#40ff9900");

	}


	/* Add a circle at (lat, lon) with specified radius. Stroke and fill colors are specified
	 * as strings. Valid strings are those valid for the argument of Color.parseColor(string):
	 * for example, "#RRGGBB", "#AARRGGBB", "red", "blue", ...
	 */

	private Circle addCircle(double lat, double lon, float radius, 
			String strokeColor, String fillColor){

		if(map == null){
			Toast.makeText(this, getString(R.string.nomap_error), Toast.LENGTH_LONG).show();
			return null;
		}

		CircleOptions circleOptions = new CircleOptions()
		.center( new LatLng(lat, lon) )
		.radius( radius )
		.strokeWidth(1)
		.fillColor(Color.parseColor(fillColor))
		.strokeColor(Color.parseColor(strokeColor));

		// Add circle to map and return reference to the Circle for possible later use
		return map.addCircle(circleOptions);	

	}


	// Process clicks on markers

	@Override
	public boolean onMarkerClick(Marker marker) {
		// Remove the marker and its info window and circle if marker clicked
		marker.remove();
		if(localCircle != null)
			localCircle.remove();
		// Return true to prevent default behavior of opening info window
		return true;

	}

	// Process clicks on the marker info window

	@Override
	public void onInfoWindowClick(Marker marker) {

		// Remove marker and circle
		marker.remove();
		if(localCircle != null)
			localCircle.remove();

	}


	@Override
	public void onInit(int status) {


	}
	private boolean isKeepRunningThreadTrue(){
		return keepRunningThread;
	}



	Runnable upDateLocation = new Runnable() {

		@Override
		public void run() {
			//if(i++ < mSampleList.size()){
			//Node n = new Node(mSampleList.get(i).getLatLng());

			while(isKeepRunningThreadTrue()){
				Log.d("sandeep","i am running");
				if(currentLatLng!= null){
					Node n = new Node(currentLatLng);
					if(null== mLocationList)
						mLocationList = new FixedSizeLinkedQueue(n);
					else if(Math.abs(mLocationList.getLatestValue().latitude-currentLat)> 0.00001 
							|| Math.abs(mLocationList.getLatestValue().longitude-currentLon) > 0.00001){						
						mLocationList.addToList(n);
						Calendar c = Calendar.getInstance();
						int min = c.get(Calendar.MINUTE);
						int sec = c.get(Calendar.SECOND);
						String date;
						if(sec/10 < 1){
							date = "1/10/2013 17:" +min +":0" +sec;
						}else
							date = "1/10/2013 17:" +min +":" +sec;
						ServerAsyncTask mytask= new ServerAsyncTask(new FixedSizeLinkedQueue(mLocationList), getApplicationContext(),date);

						mytask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
					}				
					//ServerAsyncTask mytask= new ServerAsyncTask(new FixedSizeLinkedQueue(mLocationList), getApplicationContext(),mSampleList.get(i).getDate());	


				}
				try {
					Thread.sleep(mInterval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//mHandler.postDelayed(upDateLocation, mInterval);
			/*}else{
				mHandler.removeCallbacks(upDateLocation);
			}*/


		}
	};


	private class SettingsDialogFragment extends DialogFragment{
		Spinner milesSpinner;
		Spinner speechSpinner;
		int mLayoutId;
		CheckBox cbSpeech;
		DatePicker datepicker;
		TimePicker timepicker;
		ArrayAdapter<String> milesSpinnerArrayAdapter;
		ArrayAdapter<String> speechSpinnerArrayAdapter;
		String[] arrayMiles = {"1", "2","3", "4","5","6","7","8","9","10", "11","12","13","14","15","16","17","18","19","20"};
		String [] arraySpeech ={"15 sec", "30 sec", "45 sec", "1 min","2 min","3 min", "5 min"};
		public SettingsDialogFragment(int layoutId) {
			mLayoutId = layoutId;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(mLayoutId, null);
			String title;
			if(mLayoutId == R.layout.other_settings){
				title = getResources().getString(R.string.other_settings);
				milesSpinner = (Spinner)view.findViewById(R.id.spinner_distance);
				speechSpinner = (Spinner)view.findViewById(R.id.spinner_speech_frequency);
				cbSpeech = (CheckBox)view.findViewById(R.id.checkbox_speech);				
				milesSpinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item, arrayMiles);
				speechSpinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_spinner_item, arraySpeech);
				milesSpinner.setAdapter(milesSpinnerArrayAdapter);
				speechSpinner.setAdapter(speechSpinnerArrayAdapter);
				milesSpinner.setSelection(Utils.getLookAheadMiles());
				cbSpeech.setChecked(Utils.isTTSactivated());
				speechSpinner.setSelection(Utils.getTTSfrequency());
				speechSpinner.setEnabled(cbSpeech.isChecked());
				cbSpeech.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						speechSpinner.setClickable(isChecked);
						speechSpinner.setEnabled(isChecked); 

					}
				});

			}else if(mLayoutId == R.layout.choose_date){
				title = getResources().getString(R.string.choose_date);
				datepicker =(DatePicker)view.findViewById(R.id.date_picker);
				datepicker.init(Utils.getCalendar(Calendar.YEAR), Utils.getCalendar(Calendar.MONTH), Utils.getCalendar(Calendar.DAY_OF_MONTH), null);
			}else {
				title = getResources().getString(R.string.choose_time);
				timepicker =(TimePicker)view.findViewById(R.id.time_picker);	
				timepicker.setCurrentHour(Utils.getCalendar(Calendar.HOUR_OF_DAY));
				timepicker.setCurrentMinute(Utils.getCalendar(Calendar.MINUTE));
			}
			builder.setView(view)
			.setTitle(title)
			.setPositiveButton("Save", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(mLayoutId){
					case R.layout.chhose_time:	
						Utils.setcalendar(Calendar.HOUR_OF_DAY, timepicker.getCurrentHour());
						Utils.setcalendar(Calendar.MINUTE, timepicker.getCurrentMinute());
						dismiss();
						break;
					case R.layout.choose_date:
						Utils.setcalendar(Calendar.DAY_OF_MONTH, datepicker.getDayOfMonth());
						Utils.setcalendar(Calendar.MONTH,datepicker.getMonth());
						Utils.setcalendar(Calendar.YEAR, datepicker.getYear());
						dismiss();
						break;
					case R.layout.other_settings:
						Utils.setLookAheadMiles(milesSpinner.getSelectedItemPosition());
						Utils.activateTTS(cbSpeech.isChecked());
						Utils.setTTSfrequency(speechSpinner.getSelectedItemPosition());
						dismiss();
						break;
					default: 
						break;
					}

				}
			})
			.setNegativeButton("Cancel", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismiss();

				}
			});


			return builder.create();
		}

	}


}
