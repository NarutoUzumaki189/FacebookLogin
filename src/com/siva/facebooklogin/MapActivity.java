package com.siva.facebooklogin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.Callback;
import com.facebook.Response;
import com.facebook.Session;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends FragmentActivity {

	private GoogleMap mMap;
	private PinImages mPingImages = null;
	private LatLng mLatLng;
	private ArrayList<HashMap<String , Object>> mFreindsData;
	private String mID = null;
	private Dialog mDialog = null;
	
	private final int DOWNLOAD_DATA = 0;
	private final int MAP_DATA      = 1;

	HttpClient mClient = new DefaultHttpClient();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_activity);
		if(mPingImages == null)
		{
			mPingImages = new PinImages();
		}
		if(mFreindsData == null)
		{
			mFreindsData = new ArrayList<HashMap<String,Object>>();
		}
		mID = getIntent().getStringExtra("id");
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

	private void setImages()
	{
		final Session localsession = Session.getActiveSession();

		Bundle nameBundle   = new Bundle();
		nameBundle.putString("fields", "friends.fields(id)");


		if(localsession.isOpened())
		{
			//mPingImages.execute(null,localsession);
			
			Message msg = Message.obtain();
			msg.what = DOWNLOAD_DATA;
			downloadHandler.sendMessage(msg);
			
		}
	}


	private void showErrorDialog(String error)
	{

		/*	if(mDialog == null)
		{
			mDialog = new Dialog(getBaseContext());
		}
       mDialog.setTitle(error);
       mDialog.show();*/
	}

	private JSONObject newMethod(String graphPath,Bundle inputBundle,String accessToken) throws ClientProtocolException,IOException,JSONException
	{
		JSONObject result = null;
		String data = inputBundle.getString("fields");
		String url = "https://graph.facebook.com/"+graphPath+"?access_token="+accessToken+"&fields="+data;
		Log.d("MapActivity","url::"+url);
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
			Log.d("MapActivity","result::"+result);
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
				 Bundle dataBundle = msg.getData();
				 LatLng position = dataBundle.getParcelable("location");
				 BitmapDescriptor icon =  BitmapDescriptorFactory.fromBitmap((Bitmap) dataBundle.getParcelable("image"));
				 mMap.addMarker(new MarkerOptions().position(position).icon(icon));
				 
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
			JSONArray friendsId   = newMethod("me",nameBundle,session.getAccessToken()).optJSONObject("friends").getJSONArray("data");

            int length = friendsId.length();
            
			for(int i=0;i<length;i++ )
			{
				getResponse(newMethod((String)friendsId.getJSONObject(i).get("id"),  friendBundle,session.getAccessToken()),session,location);
				
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
	
	private void getResponse(JSONObject jsonObject,Session session,Bundle location) throws JSONException, ClientProtocolException, IOException
	{
		String url = jsonObject.optJSONObject("picture").optJSONObject("data").getString("url");
		if(jsonObject.optJSONObject("location") != null)
		{
			//HashMap<String , Object> data = new HashMap<String,Object>();
			String locationID = jsonObject.optJSONObject("location").getString("id");
			LatLng position = getLocation(newMethod(locationID, location, session.getAccessToken()));
			Bitmap icon = ProfileActivity.getImage(url);
			Message msg = Message.obtain();
			msg.what = MAP_DATA;
			Bundle bundle = new Bundle();
			bundle.putParcelable("image", icon);
			bundle.putParcelable("location", position);
			msg.setData(bundle);
			downloadHandler.sendMessage(msg);
		}
		else
		{

		}

	}
	
	private LatLng getLocation(JSONObject jsonObject) throws JSONException
	{

		String lat = jsonObject.optJSONObject("location").getString("latitude");
		String lng = jsonObject.optJSONObject("location").getString("longitude");
		mLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
		return mLatLng;
	}

	private class PinImages extends AsyncTask<Object, Void, Void>
	{

		@Override
		protected Void doInBackground(Object... arg0) {
			// TODO Auto-generated method stub
			Session localSession = (Session)(arg0[1]);
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			// TODO Auto-generated method stub

			mapData();

		}
		
		
		private void mapData()
		{
			if(mFreindsData == null || mFreindsData.size() < 0)
				return ;
			int size = mFreindsData.size();
			Log.d("ProfileActivity", "I came here "+size);
			for(int i=0 ; i<size ; i++)
			{
				HashMap<String,Object> data = mFreindsData.get(i);
				LatLng position = (LatLng)data.get("position");

				BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap((Bitmap)data.get("image"));


				
			}
		}



		private void getClosure(LatLng position)
		{
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(position)
					.zoom(15.5f)
					.bearing(0)
					.tilt(25)
					.build()

					));
		}

		

		private void getResponse(final String id , final Session session)
		{
			Bundle details = new Bundle();


			details.putString("fields", "picture,location");

			new Request(session, id ,details,HttpMethod.GET ,new Callback() {

				@Override
				public void onCompleted(Response response) {
					// TODO Auto-generated method stub

					JSONObject jsonObject;

					try {
						jsonObject = new JSONObject(response.getGraphObject().getInnerJSONObject().toString());
						String url = jsonObject.optJSONObject("picture").optJSONObject("data").getString("url");
						Log.d("Response","ID:::"+id+"url"+url);
						if(jsonObject.optJSONObject("location") != null)
						{
							HashMap<String , Object> data = new HashMap<String,Object>();
							String locationID = jsonObject.optJSONObject("location").getString("id");
							data.put("image" , ProfileActivity.getImage(url));
							data.put("position", getLocation(locationID, session));
							mFreindsData.add(data);
						}
						else
						{

						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}).executeAndWait();


		}

		

		private LatLng getLocation(String graphPath,Session session)
		{
			Bundle location = new Bundle();


			location.putString("fields", "location");
			new Request(session, graphPath,location,HttpMethod.GET ,new Callback() {

				@Override
				public void onCompleted(Response response) {
					// TODO Auto-generated method stub
					Log.d("MapActivity",""+response);
					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(response.getGraphObject().getInnerJSONObject().toString());
						String lat = jsonObject.optJSONObject("location").getString("latitude");
						String lng = jsonObject.optJSONObject("location").getString("longitude");
						mLatLng = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}).executeAndWait();

			return mLatLng;
		}
	}

}
