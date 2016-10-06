package lab.drys.rufius.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;

import lab.drys.rufius.MainActivity;
import lab.drys.rufius.MainActivityReceiver;
import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;

/**
 * Created by lykanthrop on 6/23/15.
 */
public class SwitcherService extends IntentService
{
	public SwitcherService()
	{
		super("SwitcherService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{

		Rufius.logDebug("Switch Service Started");
		super.onStartCommand(intent,flags,startId);

		available = false;

		return START_STICKY;
	}

	@Override
	public void onHandleIntent(Intent intent)
	{
		context = this.getApplicationContext();
		commonPreferences = context.getSharedPreferences(Rufius.statik, Context.MODE_PRIVATE);

		Rufius.logDebug("Switch Service Handling");
		String networkId = Rufius.getSSIDBSSID(context);

		if(networkId!=null)
		{
			String storedNetworkId = commonPreferences.getString(Rufius.unit_code, null);

			SharedPreferences unitPreferences = null;
			if((storedNetworkId!=null) && (!storedNetworkId.isEmpty()))
			{
				File storedUnit = new File(context.getApplicationInfo().dataDir+"/shared_prefs/"+storedNetworkId+".xml");

				if(storedUnit.exists())
				{
					unitPreferences = context.getSharedPreferences(storedNetworkId,Context.MODE_PRIVATE);
				}
			}

			if((storedNetworkId==null) || (!storedNetworkId.equals(networkId)))
			{
				if(networkId.isEmpty()) //Mobile Change
				{
					Rufius.logDebug("Changing possibly to remote...");

					if(unitPreferences!=null)
					{
						String time = unitPreferences.getString(Rufius.time, null);
						int sleepLoops = 20;
						if(time!=null)
						{
							sleepLoops = Rufius.getGraceTime(time);
						}
						int loops = 0;

						while(loops<sleepLoops)
						{
							Rufius.logDebug("Loop " + loops);
							try
							{
								Thread.sleep(15000);
							}
							catch(InterruptedException e)
							{
								Rufius.logWarning(e.getMessage());
							}

							networkId = Rufius.getSSIDBSSID(context);

							if((networkId==null) || (!networkId.isEmpty()))
							{
								break;
							}

							loops++;
						}
					}

					if((networkId!=null) && (networkId.isEmpty()))  //Network switch to mobile
					{
						Rufius.logDebug("Changing to remote...");

						commonPreferences.edit().putString(Rufius.unit_code, "").apply();

						if((unitPreferences!=null))
						{
							this.turnOn(storedNetworkId, unitPreferences.getBoolean(Rufius.auto, false), !unitPreferences.getBoolean(Rufius.auth, true));
						}
					}
				}
				else//Wifi change
				{
					Rufius.logDebug("Changing to wifi...");

					if((unitPreferences!=null))
					{
						this.turnOn(storedNetworkId, unitPreferences.getBoolean(Rufius.auto, false), !unitPreferences.getBoolean(Rufius.auth, true));
					}

					if(!networkId.isEmpty())
					{
						File currentUnit = new File(context.getApplicationInfo().dataDir+"/shared_prefs/"+networkId+".xml");
						if(currentUnit.exists())
						{
							commonPreferences.edit().putString(Rufius.unit_code, networkId).apply();
							SharedPreferences currentUnitPreferences = context.getSharedPreferences(networkId,Context.MODE_PRIVATE);
							this.turnOff(networkId, currentUnitPreferences.getBoolean(Rufius.auto, false), !currentUnitPreferences.getBoolean(Rufius.auth, true));
						}
						else
						{
							commonPreferences.edit().putString(Rufius.unit_code,"").apply();
						}
					}
					else
					{
						commonPreferences.edit().putString(Rufius.unit_code,"").apply();
					}
				}
			}
		}

		try
		{
			Thread.sleep(2000);
		}
		catch(InterruptedException xcpt)
		{
			Rufius.logDebug(xcpt.getMessage());
		}

		available = true;

		this.stopSelf();
	}

	@Override
	public void onDestroy()
	{
		commonPreferences.edit().putBoolean(Rufius.busy,false).apply();
		super.onDestroy();
	}

	public static boolean isAvailable()
	{
		return available;
	}

	private void turnOff(String unit, boolean auto, boolean askPassword)
	{
		if(auto)
		{
			Rufius.logDebug("Turning off...");

			Intent intent0 = new Intent(context, SShService.class);
			intent0.putExtra(Rufius.unit_code,unit);
			intent0.putExtra(Rufius.command, Rufius.COM_INWIFI);
			context.startService(intent0);
		}
		else
		{
			this.notifyChange(unit, Rufius.COM_INWIFI,askPassword);
		}
	}

	private void turnOn(String unit, boolean auto, boolean askPassword)
	{
		if(auto)
		{
			Rufius.logDebug("Turning on from network change to mobile...");

			Intent intent0 = new Intent(context, SShService.class);
			intent0.putExtra(Rufius.unit_code,unit);
			intent0.putExtra(Rufius.command, Rufius.COM_OUTWIFI);
			context.startService(intent0);
		}
		else
		{
			this.notifyChange(unit, Rufius.COM_OUTWIFI,askPassword);
		}
	}

	private void notifyChange(String unitFile, String command, boolean askPassword)
	{
		Bundle bundle = new Bundle();
		bundle.putString(Rufius.unit_code, unitFile);
		bundle.putBoolean(Rufius.auto, false);
		bundle.putString(Rufius.command, command);
		if(askPassword)
		{
			bundle.putBoolean(Rufius.auth,true);
		}

		if(Rufius.isVisible())
		{
			Intent intentOut = new Intent(MainActivityReceiver.SHOW_DIALOG);
			intentOut.putExtra(Rufius.auto,bundle);
			LocalBroadcastManager.getInstance(context).sendBroadcast(intentOut);
		}
		else
		{
			Notification.Builder builder = new Notification.Builder(context);
			builder.setLights(0xff00ff00, 300, 100);
			builder.setContentTitle("Rufius Info");
			builder.setSmallIcon(R.drawable.ic_stat_notify);

			builder.setContentInfo("Permission Required");

			Intent intentOut = new Intent(context, MainActivity.class);

			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(intentOut);

			PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, builder.build());
		}
	}

	//Variables
	private static boolean available = true;
	private Context context;
	private SharedPreferences commonPreferences;
}
