package com.siva.facebooklogin;



import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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


	public static final String LOCATIONID = "locationID";
	
	ImageView mProfileImage;
	TextView mFirstName,mLastName;
	String mLocationID = null;
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
		nameBundle.putString("fields", "first_name,middle_name,last_name,picture.width(200).height(200),location");

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
						mLocationID = jsonObject.optJSONObject("location").getString("id");
						mDownloadTask.execute(jsonObject.optJSONObject("picture").optJSONObject("data").getString("url"),jsonObject.getString("first_name"),jsonObject.getString("last_name"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Toast.makeText(getBaseContext(), "Some Error Came", Toast.LENGTH_LONG).show();
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
			Bitmap image = Utility.getBitmap(arg0[0]);
			data.put("image", image);
			data.put("firstname", arg0[1]);
			data.put("lastname", arg0[2]);
			mFirstName.setText(arg0[1]);
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
		Intent intent = new Intent(ProfileActivity.this, MapActivity.class);
		intent.putExtra(LOCATIONID, mLocationID);
		startActivity(intent);

	}

}
