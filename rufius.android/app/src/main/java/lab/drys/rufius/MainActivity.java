package lab.drys.rufius;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.File;

import lab.drys.rufius.services.GoogleWorkerService;
import lab.drys.rufius.unit.UnitPreferencesActivity;
import lab.drys.rufius.unit.UnitsListFragment;

/**
 * Created by lykanthrop on 6/2/15.
 */
public class MainActivity extends RufiusActivityBasic
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.activity_main);

		pendingIntentOnOK = null;

		progressSpin = (ProgressBar) this.findViewById(R.id.main_progress_spin);
		progressSpin.setVisibility(View.INVISIBLE);

		receiver = new MainActivityReceiver(this);

		receiverFilter = new IntentFilter();
		receiverFilter.addAction(MainActivityReceiver.SET_BUSY);
		receiverFilter.addAction(MainActivityReceiver.UPDATE_STATUS);
		receiverFilter.addAction(MainActivityReceiver.GOOGLE_PERMISSION);
		receiverFilter.addAction(MainActivityReceiver.UPDATE_LIST_STATUS);
		receiverFilter.addAction(MainActivityReceiver.UPDATE_LIST_VIEW);
		receiverFilter.addAction(MainActivityReceiver.SET_LIST_BUNDLE);
		receiverFilter.addAction(MainActivityReceiver.GOOGLE_IMAGE);
		receiverFilter.addAction(MainActivityReceiver.RESET_COMMON);

		accountIcon = (ImageView)this.findViewById(R.id.drawer_image);
		nameText = (TextView)this.findViewById(R.id.drawer_gmailname);
		gmailText = (TextView)this.findViewById(R.id.drawer_gmail);

		toolbar = (Toolbar)this.findViewById(R.id.main_toolbar);
		toolbar.showOverflowMenu();
		toolbar.setNavigationIcon(R.drawable.ic_launcher);
		toolbar.setTitle("");
		this.setSupportActionBar(toolbar);

		unitsFragment = new UnitsListFragment();
		getFragmentManager().beginTransaction().replace(R.id.list_frame, unitsFragment).commit();
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);

		mainDrawer = new MainDrawer(this);
	}

	@Override
	public void onStart()
	{
		super.onStart();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if(!commonPreferences.getBoolean(Rufius.ready, false))
		{
			this.firstRun();
		}

		this.setGoogleInfo(null);
		this.updateList();

		checkSpot = commonPreferences.getBoolean(Rufius.sync,false);

		if(checkSpot)
		{
			this.spotCheck();
			this.updateStatus();
		}

		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, receiverFilter);

		Rufius.mainActivityResumed();
	}

	@Override
	public void onPause()
	{
		Rufius.mainActivityPaused();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

		super.onPause();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu mn)
	{
		menu = mn;

		getMenuInflater().inflate(R.menu.main_activity_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu mn)
	{
		if(menu!=null)
		{
			String fileName = Rufius.getSSIDBSSID(this);
			if(fileName==null || fileName.isEmpty() || unitExists(fileName))
			{
				menu.findItem(R.id.unit_new_from_con).setVisible(false);
			}
			else
			{
				menu.findItem(R.id.unit_new_from_con).setVisible(true);
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int caseId = item.getItemId();

		if(caseId == R.id.unit_new_from_con)
		{
			String fileName = Rufius.getSSIDBSSID(this);
			if((fileName!=null) && (!fileName.isEmpty()))
			{
				File file = new File(this.getApplicationInfo().dataDir+"/shared_prefs/"+fileName+".xml");

				Rufius.logInfo(file.getAbsolutePath());

				if(file.exists())
				{
					this.showToastInfo("Network Exists");
				}
				else
				{
					Intent intent = new Intent(this, UnitPreferencesActivity.class);
					intent.putExtra(Rufius.unit_code,fileName);
					intent.putExtra(Rufius.ready,false);
					startActivityForResult(intent, Rufius.REQUEST_CODE_CREATE_NEW_UNIT);
				}
			}
		}
		/*else if(caseId == R.id.unit_new)
		{

		}*/
		else if(caseId == android.R.id.home)
		{
			if(!mainDrawer.isDrawerOpen())
			{
				mainDrawer.openDrawer();
			}
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == Rufius.REQUEST_CODE_CREATE_NEW_UNIT)
		{
			String fileName = data.getStringExtra(Rufius.unit_code);
			if(resultCode == RESULT_OK)
			{
				unitsFragment.add(this.getSharedPreferences(fileName, MODE_PRIVATE));

			}
			else if(resultCode == RESULT_CANCELED)
			{
				this.getSharedPreferences(fileName,MODE_PRIVATE).edit().clear().apply();

				File file = new File(this.getApplicationInfo().dataDir+"/shared_prefs/"+fileName+".xml");

				if(!file.delete())
				{
					this.showToastInfo("List Contains Errors");
				}

				file = new File(this.getApplicationInfo().dataDir+"/shared_prefs/"+fileName+".bak");

				if(file.exists())
				{
					boolean bl = file.delete();
					if(!(bl|| Rufius.RELEASE_FLAG))
					{
						Rufius.logInfo("File *.bak Deletion Error");
					}
				}
			}
		}
		else if(requestCode == Rufius.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR && resultCode == RESULT_OK)
		{
			if(pendingIntentOnOK!=null)
			{
				Intent intentOut = new Intent(this,GoogleWorkerService.class);
				intentOut.putExtras(pendingIntentOnOK);
				this.startService(intentOut);
			}
			pendingIntentOnOK = null;
		}
		else if(requestCode == Rufius.REQUEST_CODE_PICK_ACCOUNT && resultCode == RESULT_OK)
		{
			String mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			commonPreferences.edit().putString(Rufius.user_gmail, mEmail).apply();
			Intent intentOut = new Intent(this,GoogleWorkerService.class);
			intentOut.putExtra(Rufius.user_gmail,commonPreferences.getString(Rufius.user_gmail,null));
			intentOut.putExtra(Rufius.user, true);
			this.startService(intentOut);
			mainDrawer.setSignedIn(true);
		}
	}

	public void pickGmailAccount()
	{
		String[] accountTypes = new String[]{"com.google"};
		Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
		this.startActivityForResult(intent, Rufius.REQUEST_CODE_PICK_ACCOUNT);
	}

	public void handlePlayServicesAvailabilityException(int statusCode)
	{
		// The Google Play services APK is old, disabled, or not present.
		// Show a dialog created by Google Play services that allows
		// the user to update the APK
		// int statusCode = ((GooglePlayServicesAvailabilityException) e).getConnectionStatusCode();
		Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode, this, Rufius.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
		dialog.show();
	}

	public void handleRecoverableAuthException(Intent intent)
	{
		// Unable to authenticate, such as when the user has not yet granted
		// the app access to the account, but the user can fix this.
		// Forward the user to an activity in Google Play services.
		// Intent intent = ((UserRecoverableAuthException) e).getIntent();
		startActivityForResult(intent, Rufius.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
	}

	public void setPendingIntentOnOK(Intent intent)
	{
		pendingIntentOnOK = (Intent)intent.clone();
	}

	public void setBusy(boolean b)
	{
		if(b)
		{
			progressSpin.setVisibility(View.VISIBLE);
			unitsFragment.getListView().setEnabled(false);
			unitsFragment.endActionMode();
			this.onPrepareOptionsMenu(menu);
		}
		else
		{
			progressSpin.setVisibility(View.GONE);
			unitsFragment.getListView().setEnabled(true);
			this.onPrepareOptionsMenu(menu);
		}
	}

	public void setGoogleInfo(String str)
	{
		if((str!=null))
		{
			commonPreferences.edit().putString(Rufius.user,str).apply();
			nameText.setText(str);
		}
		else
		{
			str = commonPreferences.getString(Rufius.user,null);
			if(str!=null)
			{
				nameText.setText(str);
			}
		}

		str = commonPreferences.getString(Rufius.user_gmail,null);

		if((str!=null) && (!str.isEmpty()))
		{
			gmailText.setText(str);
			str = this.getApplicationInfo().dataDir+"/"+str+".png";

			File fl = new File(str);

			if(fl.exists())
			{
				Bitmap bmp = BitmapFactory.decodeFile(str);

				accountIcon.setImageBitmap(bmp);
			}
		}
		else
		{
			accountIcon.setImageResource(R.drawable.ic_account_circle_black_48dp);
			gmailText.setText("");
		}
	}

	public void clearCommonPreferences()
	{
		File file = new File(this.getApplicationInfo().dataDir+"/shared_prefs/statik.xml");

		if(file.exists())
		{
			file.delete();
		}

		commonPreferences = this.getSharedPreferences(Rufius.statik,MODE_PRIVATE);
		this.firstRun();
		this.setGoogleInfo("");
		mainDrawer.setSignedIn(false);

		this.updateStatus();
	}

	private void firstRun()
	{
		if(!Rufius.RELEASE_FLAG)
		{
			Rufius.logInfo("First Time Running");
		}

		SharedPreferences.Editor editor = commonPreferences.edit();
		editor.putString(Rufius.user_gmail, "");
		editor.putBoolean(Rufius.ready, true);
		editor.putBoolean(Rufius.busy, false);
		editor.putString(Rufius.user, "");
		editor.putString(Rufius.unit_code, Rufius.getSSIDBSSID(this));
		editor.putBoolean(Rufius.sync,false);
		editor.apply();


		File newFile = new File(this.getFilesDir().getAbsolutePath() + "/known_hosts");
		if(newFile.mkdir())
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logInfo("Known Hosts Directory created");
			}
		}

		newFile = new File(this.getFilesDir().getAbsolutePath()+"/snapshots");

		if(newFile.mkdir())
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logInfo("Snapshots Directory created");
			}
		}

		newFile = new File(this.getFilesDir().getAbsolutePath()+"/triggers");

		if(newFile.mkdir())
		{
			if (!Rufius.RELEASE_FLAG)
			{
				Rufius.logInfo("Snapshots Directory created");
			}
		}
	}

	public boolean unitExists(String unitName)
	{
		File fl = new File(this.getApplicationInfo().dataDir+"/shared_prefs/"+unitName+".xml");

		return fl.exists();
	}

	public void updateStatus()
	{
		unitsFragment.updateList();
	}

	public void endActionMode()
	{
		unitsFragment.endActionMode();
	}


	public void declarePresence()
	{
		unitsFragment.spotCheck();
	}

	public void closeDrawer(View v)
	{
		mainDrawer.closeDrawer();
	}

	public void updateList()
	{
		unitsFragment.createList();
	}

	public void spotCheck()
	{
		if(commonPreferences!=null)
		{
			unitsFragment.spotCheck();
		}
	}

	public void setListItemBundle(String unit, Bundle bundle)
	{
		unitsFragment.set(unit, bundle);
	}

	//Variables
	private static BroadcastReceiver receiver;
	private static IntentFilter receiverFilter;
	private static Intent pendingIntentOnOK;
	private static Menu menu;
	private static MainDrawer mainDrawer;
	private static ImageView accountIcon;
	private static UnitsListFragment unitsFragment;
	private static Toolbar toolbar;
	private static TextView nameText;
	private static TextView gmailText;
	private static ProgressBar progressSpin;

	private static boolean checkSpot;
}
