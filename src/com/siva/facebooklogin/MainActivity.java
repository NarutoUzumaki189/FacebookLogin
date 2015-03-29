package com.siva.facebooklogin;

import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.UserSettingsFragment;


public class MainActivity extends FragmentActivity {
	private UserSettingsFragment userSettingsFragment;
	private UiLifecycleHelper helper;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);


		Session session = Session.getActiveSession();

		if(session != null && session.getState() == SessionState.OPENED)
		{
			startProfileActivity();
		}

		helper = new UiLifecycleHelper(this, loginCallback);

		FragmentManager fragmentManager = getSupportFragmentManager();
		userSettingsFragment = (UserSettingsFragment) fragmentManager.findFragmentById(R.id.login_fragment);
		userSettingsFragment.setReadPermissions(Arrays.asList("basic_info","email","friends_location"));
		userSettingsFragment.setSessionStatusCallback(loginCallback);
	}

	private Session.StatusCallback loginCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			Log.d("MainActivity", String.format("New session state: %s", state.toString()));
			if(state == SessionState.OPENED)
			{

				startProfileActivity();
			}
		}

	};
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			userSettingsFragment.onActivityResult(requestCode, resultCode, data);
			super.onActivityResult(requestCode, resultCode, data);
		}

		@Override
		public void onResume()
		{
			super.onResume();
			helper.onResume();

		}

		private void startProfileActivity()
		{
			finish();
			setResult(RESULT_OK, null);
			Intent intent = new Intent(MainActivity.this , ProfileActivity.class);
			startActivity(intent);

		}
		
		@Override
		protected void onDestroy()
		{
			super.onDestroy();
			helper.onDestroy();
		}

	}