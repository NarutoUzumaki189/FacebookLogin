package com.siva.facebooklogin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.animation.BounceInterpolator;

import com.facebook.Session;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity implements MappingInterface {

	private GoogleMap mMap;
	private LatLng mLatLng = null;
	private LatLng mUserLocation;
	private final String TAG = "MapActivity";
	private static HashMap<String ,LatLng> mFreindsPosition;
	private static ArrayList<String> mFreindsID;
	private HashMap<String,LatLng> mLocationCache ;

	private int count = 0;

	private int offset = 3;
	
	private boolean moveMap = true;

	private GetUserLocationTask mGetUserLocationTask = null;

	private boolean lastcall = false;

	private Long mTotalTime = (long) 0 ;

	private final int DOWNLOAD_DATA = 0;
	private final int MAP_DATA      = 1;
	private String userLocationID = null; 

	HttpClient mClient ;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mTotalTime = System.currentTimeMillis();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_activity);

		Intent intent = getIntent();

		if(intent != null)
		{
			userLocationID = intent.getStringExtra(ProfileActivity.LOCATIONID);
		}

		if(mGetUserLocationTask == null)
		{
			mGetUserLocationTask = new GetUserLocationTask();
		}

		if(Utility.model == null) {
			Utility.model = new FriendsGetProfilePics();
			Utility.model.friendsImages.setAddListener(this);
		}

		if(mClient == null) {
			mClient = getThreadSafeClient();
		}

		if(mFreindsPosition == null)
		{
			mFreindsPosition = new HashMap<String ,LatLng>();
		}
		if(mFreindsID == null)
		{
			mFreindsID = new ArrayList<String>();
		}

		if(mLocationCache == null)
		{
			mLocationCache = new HashMap<String, LatLng>();
		}

		mGetUserLocationTask.execute(userLocationID);
		setmap();
		setImages();
		
	}

	private void setmap() {



		// TODO Auto-generated method stub
		if(mMap == null)
		{
			FragmentManager manager = getSupportFragmentManager();
			mMap = ((SupportMapFragment) manager.findFragmentById(R.id.map)).getMap();
		}


	}

	private LatLng getUserLocation()
	{
		final Bundle locationBundle = new Bundle();
		locationBundle.putString("fields", "location");
		try {

			mUserLocation = getLocation(userLocationID,getJsonObject(userLocationID, locationBundle, Session.getActiveSession().getAccessToken()));

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mUserLocation;

	}
	
	
	private void animateMarkers(final Marker marker)
	{

		final Handler handler = new Handler();
        
        final long startTime = SystemClock.uptimeMillis();
        final long duration = 2000;
        
        Projection proj = mMap.getProjection();
        final LatLng markerLatLng = marker.getPosition();
        Point startPoint = proj.toScreenLocation(markerLatLng);
        startPoint.offset(0, -100);
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

        final BounceInterpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
                
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
	}

	private void setImages()
	{
		final Session localsession = Session.getActiveSession();

		Bundle nameBundle   = new Bundle();
		nameBundle.putString("fields", "friends.fields(id)");


		if(localsession.isOpened())
		{


			if(Utility.model.friendsImages.isEmpty())
			{
				Message msg = Message.obtain();
				msg.what = DOWNLOAD_DATA;
				downloadHandler.sendMessage(msg);
			}
			else
			{
				Message msg = Message.obtain();
				msg.what = MAP_DATA;
				downloadHandler.sendMessage(msg);
			}

		}
	}


	private void showErrorDialog(String error)
	{
		
	}

	private JSONObject getJsonObject(String graphPath,Bundle inputBundle,String accessToken) throws ClientProtocolException,IOException,JSONException
	{
		long duration = System.currentTimeMillis();
		JSONObject result = null;
		String data = inputBundle.getString("fields");
		String url = "https://graph.facebook.com/"+graphPath+"?access_token="+accessToken+"&fields="+data;
		HttpGet get = new HttpGet(url);
		HttpResponse response = mClient.execute(get);
		int resultCode = response.getStatusLine().getStatusCode();

		if(resultCode == 200)
		{
			InputStream is = response.getEntity().getContent() ;
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			String resultString = sb.toString();

			result = new JSONObject(resultString);
			Log.d("newMethod", ""+(System.currentTimeMillis()-duration));
			return result;
		}
		else
		{
			Log.d("MapActivity","Failed");
			return null;
		}
	}

	private Handler downloadHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub

			switch (msg.what)
			{

			case DOWNLOAD_DATA:
				Thread thread = new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						getInformation(Session.getActiveSession());
					}
				});
				thread.start();
				break;
			case MAP_DATA:


				int size = 0;
				if(!Utility.model.friendsImages.isEmpty())
				{
					size = Utility.model.friendsImages.size();
					for(int i=count ; i < size ; i++ )
					{
						LatLng position = mFreindsPosition.get(i);


						if(i > 46)
						{
							Log.d(TAG,"index::"+i+"position:::"+position);
						}



						BitmapDescriptor icon =  BitmapDescriptorFactory.fromBitmap((Bitmap) Utility.model.friendsImages.get(mFreindsID.get(i)));
						try
						{
							Marker marker = mMap.addMarker(new MarkerOptions().position(position).icon(icon));
							
							
							
							animateMarkers(marker);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}


				}

				if(moveMap && mUserLocation != null )
				{


					getClosure(mUserLocation);

					moveMap = false;
				}

				count = size;
				if(lastcall)
				{
					Log.d("TotalTime",""+(System.currentTimeMillis()-mTotalTime));
				}

				break;

			}

		}

	};

	private void getInformation(Session session)
	{
       
		

		Bundle nameBundle   = new Bundle();
		nameBundle.putString("fields", "friends.fields(id)");

		Bundle friendBundle = new Bundle();
		friendBundle.putString("fields", "picture,location");

		Bundle location = new Bundle();
		location.putString("fields", "location");

		try {
			JSONArray friendsId   = getJsonObject("me",nameBundle,session.getAccessToken()).optJSONObject("friends").getJSONArray("data");

			int length = friendsId.length();

			for(int i=0;i<length; i++ )
			{
				String id = (String)friendsId.getJSONObject(i).get("id");
				if(i == length-1)
				{
					lastcall = true;
				}
				getResponse(id,getJsonObject(id , friendBundle,session.getAccessToken()),session,location);

			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showErrorDialog("Protocol Error");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showErrorDialog("IO Exception");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			showErrorDialog("JSONException");
		}



	}

	private void getResponse(String id ,JSONObject jsonObject,Session session,Bundle locationBundle) throws JSONException, ClientProtocolException, IOException
	{
		String url = jsonObject.optJSONObject("picture").optJSONObject("data").getString("url");
		if(jsonObject.optJSONObject("location") != null)
		{
			long duration = System.currentTimeMillis();
			LatLng location = null;
			String locationID = jsonObject.optJSONObject("location").getString("id");
			if(checkLocationCached(locationID))
			{
				location = getLocation(locationID);
			}
			else
			{
				location = getLocation(locationID , getJsonObject(locationID, locationBundle, session.getAccessToken()));
			}

			mFreindsPosition.put(id, location);
			Utility.model.getImage(id, url);
			mFreindsID.add(id);
			Log.d("getResponse", ""+(System.currentTimeMillis()-duration));
		}
		else
		{

		}

	}


	private boolean checkLocationCached(String ID) {
		// TODO Auto-generated method stub
		if(mLocationCache != null)
		{
			Set<String> tempSet = mLocationCache.keySet();
			return tempSet.contains(ID);
		}
		return false;
	}

	private LatLng getLocation(String ID) {
		// TODO Auto-generated method stub
		LatLng newLocation = null;
		LatLng cachedLocation = null;
		cachedLocation = mLocationCache.get(ID);
		double latitude  = cachedLocation.latitude + (offset * 0.002f);
		double longitude = cachedLocation.longitude + (offset * 0.002f);
		newLocation = new LatLng(latitude, longitude);
		mLocationCache.remove(ID);
		mLocationCache.put(ID,newLocation);
		return newLocation;
	}

	private LatLng getLocation(String ID , JSONObject jsonObject) throws JSONException
	{
		long duration = System.currentTimeMillis();
		String lat = jsonObject.optJSONObject("location").getString("latitude");
		String lng = jsonObject.optJSONObject("location").getString("longitude");
		mLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
		mLocationCache.put(ID , mLatLng);
		Log.d("getLocation", ""+(System.currentTimeMillis()-duration));
		return mLatLng;
	}

	private class GetUserLocationTask extends AsyncTask<String,Void,LatLng>
	{

		@Override
		protected LatLng doInBackground(String... params) {
			// TODO Auto-generated method stub
			return getUserLocation();
		}

		@Override
		protected void onPostExecute(LatLng result) {
			// TODO Auto-generated method stub
		}



	}

	private void getClosure(LatLng position)
	{
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(position)
				.zoom(1.0f)
				.bearing(0)
				.tilt(25)
				.build()

				));
	}

	public static DefaultHttpClient getThreadSafeClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				mgr.getSchemeRegistry()), params);
		return client;

	}

	@Override
	public void addMarker(String key, Bitmap value) {
		// TODO Auto-generated method stub
		Log.d("addMarker","addMarker");
		
		LatLng position = mFreindsPosition.get(key);
		BitmapDescriptor icon =  BitmapDescriptorFactory.fromBitmap(value);
		Marker marker = mMap.addMarker(new MarkerOptions().position(position).icon(icon));
		animateMarkers(marker);
	}

}
