package com.siva.facebooklogin;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.Callback;
import com.facebook.Response;
import com.facebook.Session;

public class ProfileActivity extends Activity implements OnClickListener{


	ImageView mProfileImage;
	TextView mFirstName,mLastName;
	String mID= null;
	ProgressBar mProgressbar;
	LinearLayout mImageFrame;
	Button mLocateFriends;
	DownloadTask mDownloadTask = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile_layout);
		mFirstName = (TextView)(findViewById(R.id.firstname));
		mLastName = (TextView)(findViewById(R.id.lastname));
		mProfileImage = (ImageView)(findViewById(R.id.profile_image));
		mProgressbar  = (ProgressBar)(findViewById(R.id.progressBar));
		mImageFrame   = (LinearLayout)(findViewById(R.id.image_frame));
		mLocateFriends = (Button)(findViewById(R.id.locatefriends));
		
		Session session = Session.getActiveSession();
		Bundle nameBundle   = new Bundle();
		nameBundle.putString("fields", "first_name,middle_name,last_name,picture.width(200).height(200)");
		
		if(mDownloadTask == null)
		{
			mDownloadTask = new DownloadTask();
		}
		if(session.isOpened())
		{

			new Request(session, "me",nameBundle,HttpMethod.GET ,new Callback() {

				@Override
				public void onCompleted(Response response) {
					// TODO Auto-generated method stub
					try {

						
						JSONObject jsonObject = new JSONObject(response.getGraphObject().getInnerJSONObject().toString());

						mID = jsonObject.getString("id");
						Log.d("ProfileActivity", "response::"+mID);
						

						mDownloadTask.execute(jsonObject.optJSONObject("picture").optJSONObject("data").getString("url"),jsonObject.getString("first_name"),jsonObject.getString("last_name"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}).executeAsync();



		}

		mLocateFriends.setOnClickListener(this);

	}
	public class DownloadTask extends AsyncTask<String,Integer,HashMap<String,Object>>
	{

		protected HashMap<String,Object> doInBackground(String... arg0) {
			// TODO Auto-generated method stub

			HashMap<String,Object> data = new HashMap<String, Object>();


			Log.d("ProfileActivity", "string :"+arg0[0]);

			Bitmap image = getImage(arg0[0]);
			data.put("image", image);
			data.put("firstname", arg0[1]);
			data.put("lastname", arg0[2]);
			return data;    
		}




		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub

		}

		@Override
		protected void onPostExecute(HashMap<String,Object> data) {
			// TODO Auto-generated method stub

			mProgressbar.setVisibility(View.GONE);
			if(data != null)
			{

				mProfileImage.setImageBitmap((Bitmap)data.get("image"));
				mFirstName.setText((String)data.get("firstname"));
				mLastName.setText((String)data.get("lastname"));
           
				setVisibilty(mImageFrame);
				setVisibilty(mProfileImage);
				setVisibilty(mLastName);
				setVisibilty(mFirstName);
				setVisibilty(mLocateFriends);
			}
			else
			{
				Toast.makeText(getBaseContext(), "Some Error Came", Toast.LENGTH_LONG).show();
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.logout:

			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setVisibilty(View view)
	{
		view.setVisibility(View.VISIBLE);
	}


	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub

		Log.d("ProfileActivity",""+view.getId());

		Intent intent = new Intent(ProfileActivity.this, MapActivity.class);
		intent.putExtra("id", mID);

		startActivity(intent);

	}

	public static Bitmap getImage(String imageUrl)
	{
		long duration = System.currentTimeMillis();
		Bitmap image = null;
		InputStream in = null;
		int response = -1;

		try{
			URL url = new URL(imageUrl);
			URLConnection conn = url.openConnection();

			if (!(conn instanceof HttpURLConnection))                    
				throw new IOException("Not an HTTP connection");

			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			httpConn.connect();

			response = httpConn.getResponseCode();                
			if (response == HttpURLConnection.HTTP_OK) {
				in = httpConn.getInputStream();                                
			}                    
		}
		catch (Exception ex)
		{

		}

		BufferedInputStream bi = new BufferedInputStream(in);
		image = BitmapFactory.decodeStream(bi);
        Log.d("TimeTaken", ""+(System.currentTimeMillis()-duration));
		return image;
	}


}
