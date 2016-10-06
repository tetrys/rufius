/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab.drys.rufius.unit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import lab.drys.rufius.R;
import lab.drys.rufius.RufiusActivityBasic;
import lab.drys.rufius.Rufius;

public class UnitPreferencesActivity extends RufiusActivityBasic
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_unit_preferences);

		Intent intent = this.getIntent();
		createFile = !intent.getBooleanExtra(Rufius.ready, true);

		preferenceFile = intent.getStringExtra(Rufius.unit_code);
		unitPreferences = null;
		if(preferenceFile!=null && (!preferenceFile.isEmpty()))
		{
			unitPreferences = this.getSharedPreferences(preferenceFile,MODE_PRIVATE);

			toolbar = (Toolbar)this.findViewById(R.id.main_toolbar);
			toolbar.setNavigationIcon(R.drawable.ic_launcher);

			toolbar.setTitle("Settings");

			if(unitPreferences!=null)
			{
				String str = unitPreferences.getString(Rufius.unit,"");
				if(str!=null && (!str.isEmpty()))
				{
					toolbar.setSubtitle(str);
				}
				else
				{
					toolbar.setTitle("New Unit");
				}
			}
			else
			{
				toolbar.setTitle("New Unit");
			}

			this.setSupportActionBar(toolbar);

			settings_fragment = new UnitPreferencesFragment();
			Bundle bundle = new Bundle();
			bundle.putString(Rufius.unit_code, preferenceFile);
			settings_fragment.setArguments(bundle);
			getFragmentManager().beginTransaction().replace(R.id.main_frame, settings_fragment).commit();
		}


	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(createFile)
		{
			int rc = 0x0000ffff;

			if((commonPreferences!=null))
			{
				String str = commonPreferences.getString(Rufius.user_gmail,"");
				if((str!= null) && (!str.isEmpty()))
				{
					rc = 0x00007fff;
				}
			}
			unitPreferences.edit()
					.putBoolean(Rufius.ready,false)
					.putInt(Rufius.error_code, rc)
					.putString(Rufius.unit_code,preferenceFile).apply();
		}

		if(unitPreferences!=null)
		{
			unitPreferences.registerOnSharedPreferenceChangeListener(settings_fragment);
		}
		else
		{
			this.showToastInfo("File Creation Error");
			this.setResult(RESULT_CANCELED);
			this.finish();
		}
		Rufius.preferenceActivityResumed();
	}

	@Override
	public void onPause()
	{
		Rufius.preferenceActivityPaused();

		if(unitPreferences!=null)
		{
			unitPreferences.unregisterOnSharedPreferenceChangeListener(settings_fragment);
		}

		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu mn)
	{
		menu = mn;

		getMenuInflater().inflate(R.menu.unit_preferences_activity_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int caseId = item.getItemId();

		Intent intent = new Intent();
		intent.putExtra(Rufius.unit_code, preferenceFile);
		if(caseId == R.id.unit_ok | caseId == android.R.id.home)
		{
			int errors = unitPreferences.getInt(Rufius.error_code, 0xffffffff);
			boolean stIP = unitPreferences.getBoolean(Rufius.statik,false);
			if((errors & 0x0000000f)==0)
			{
				unitPreferences.edit().putBoolean(Rufius.ready,true).apply();
				this.setResult(RESULT_OK, intent);
				this.finish();
			}
			else
			{
				this.showToastInfo("Configuration contains Errors\n"+ Rufius.generateErrorInfo(errors, stIP));
				if(!Rufius.RELEASE_FLAG)
				{
					Rufius.logError("Error Code: " + Integer.toHexString(errors));
				}
			}
		}
		else if(caseId == R.id.unit_cancel)
		{
			this.setResult(RESULT_CANCELED,intent);
			this.finish();
		}

		return true;
	}

	//Variables
	private static UnitPreferencesFragment settings_fragment;
	private static Toolbar toolbar;
	private static Menu menu;
	private static boolean createFile;
	private static String preferenceFile;
	private static SharedPreferences unitPreferences;
}
