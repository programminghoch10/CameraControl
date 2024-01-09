package com.programminghoch10.cameracontrol;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends Activity {
	
	private static boolean xposedActive = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		Log.d("CameraManager", "onCreate: xposedactive=" + xposedActive);
		if (!xposedActive) {
			setContentView(R.layout.settings_activity_noxposed);
			return;
		}
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null) {
			getFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
	}
	
	public static class SettingsFragment extends PreferenceFragment {
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName("camera");
			addPreferencesFromResource(R.xml.camera_preferences);
		}
	}
	
}