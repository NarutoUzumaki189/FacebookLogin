package com.siva.facebooklogin;

import java.io.IOException;
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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
			/*new Request(localsession, "me",nameBundle,HttpMethod.GET ,new Callback() {

				@Override
				public void onCompleted(Response response) {
					// TODO Auto-generated method stub
					

				}

			}).executeAsync();*/
			
			mPingImages.execute(null,localsession);
			
		}
	}
	
	
	private void showErrorDialog()
	{
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Some Downloading Error")
	       .setTitle("Error");
		AlertDialog dialog = builder.create();
		
		dialog.show();

	}
	
	private JSONObject newMethod(String graphPath,Bundle inputBundle,String accessToken) throws ClientProtocolException,IOException,JSONException
	{
		JSONObject result = null;
		String data = inputBundle.getString("fields");
		String url = "http://graph.facebook.com/"+graphPath+"?fields=";
		Log.d("MapActivity","url::"+url);
		HttpGet get = new HttpGet(url);
		HttpResponse response = mClient.execute(get);
		int resultCode = response.getStatusLine().getStatusCode();
		
		if(resultCode == 200)
		{
			
			result = new JSONObject(response.getEntity().toString());
			Log.d("MapActivity",""+result);
			return result;
		}
		else
		{
			Log.d("MapActivity","Failed");
			return null;
		}
	}

	private class PinImages extends AsyncTask<Object, Void, Void>
	{

		@Override
		protected Void doInBackground(Object... arg0) {
			// TODO Auto-generated method stub
			Session session    = (Session)(arg0[1]);
			/*Response response = (Response)(arg0[0]);
			
			
		
			
			try
			{
				JSONObject jsonObject = new JSONObject(response.getGraphObject().getInnerJSONObject().toString());
				JSONArray friendsId   = jsonObject.optJSONObject("friends").getJSONArray("data");
			
				Log.d("Total", "length:::"+friendsId.length());
				
				for(int i=0;i<10;i++ )
				{
					getResponse((String)friendsId.getJSONObject(i).get("id") , session);
				}
				
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			
			Bundle nameBundle   = new Bundle();
			nameBundle.putString("fields", "friends.fields(id)");
			
			try {
				newMethod(mID,nameBundle,session.getAccessToken());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showErrorDialog();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showErrorDialog();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				showErrorDialog();
			}
		
			
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			// TODO Auto-generated method stub
			
			if(mFreindsData == null || mFreindsData.size() < 0)
				return ;
			int size = mFreindsData.size();
			Log.d("ProfileActivity", "I came here "+size);
			for(int i=0 ; i<size ; i++)
			{
				HashMap<String,Object> data = mFreindsData.get(i);
				LatLng position = (LatLng)data.get("position");

				BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap((Bitmap)data.get("image"));

				
				mMap.addMarker(new MarkerOptions().position(position).icon(icon));
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