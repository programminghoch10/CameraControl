package com.programminghoch10.cameracontrol;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
	
	private static boolean xposedActive = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
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
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
	}
	
	public static class SettingsFragment extends PreferenceFragmentCompat {
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			getPreferenceManager().setSharedPreferencesName("camera");
			setPreferencesFromResource(R.xml.camera_preferences, rootKey);
		}
		
	}
	
}