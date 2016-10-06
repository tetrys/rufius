package lab.drys.rufius.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import lab.drys.rufius.MainActivityReceiver;
import lab.drys.rufius.Rufius;
import lab.drys.rufius.unit.UnitActivityReceiver;

/**
 * Created by lykanthrop on 6/5/15.
 */
public class MainService extends IntentService
{
	public MainService()
	{
		super("MainService");
	}

	public void onHandleIntent(Intent intent)
	{
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

		if(intent.getBooleanExtra(Rufius.ready,false))
		{
			Intent intent0 = new Intent(MainActivityReceiver.SET_BUSY);
			intent0.putExtra(Rufius.busy, true);
			lbm.sendBroadcast(intent0);

			ArrayList<String> filePaths = (ArrayList<String>)intent.getSerializableExtra(Rufius.unit);


			String dataDir = this.getApplicationInfo().dataDir;
			for(String str : filePaths)
			{
				this.getSharedPreferences(str,MODE_PRIVATE).edit().clear().commit();

				File file = new File(dataDir+"/shared_prefs/"+str+".xml");
				file.delete();

				file = new File(dataDir+"/shared_prefs/"+str+".bak");
				file.delete();

				file = new File(dataDir+"/files/known_hosts/"+str);
				file.delete();
			}

			intent0 = new Intent(MainActivityReceiver.UPDATE_LIST_VIEW);
			lbm.sendBroadcast(intent0);

			intent0 = new Intent(MainActivityReceiver.SET_BUSY);
			intent0.putExtra(Rufius.busy, false);
			lbm.sendBroadcast(intent0);
		}
	}
}
