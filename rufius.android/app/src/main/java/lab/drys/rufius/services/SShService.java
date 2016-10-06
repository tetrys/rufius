/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab.drys.rufius.services;

import android.app.Application;
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
import java.io.IOException;

import lab.drys.rufius.MainActivity;
import lab.drys.rufius.MainActivityReceiver;
import lab.drys.rufius.R;
import lab.drys.rufius.unit.UnitActivity;
import lab.drys.rufius.unit.UnitActivityReceiver;
import lab.drys.rufius.utilities.Commander;
import lab.drys.rufius.Rufius;

public class SShService extends IntentService
{
	public SShService()
	{
		super("Commander_Service");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Rufius.logDebug("Commander Service Started");
		super.onStartCommand(intent,flags,startId);

		return START_STICKY;
	}

	@Override
	public void onHandleIntent(Intent intent)
	{
		int commanderStatus = 0x0fff0000;

		String unitFile = intent.getStringExtra(Rufius.unit_code);

		SharedPreferences commonPreferences = this.getSharedPreferences(Rufius.statik, MODE_PRIVATE);
		SharedPreferences unitPreferences = this.getSharedPreferences(unitFile, MODE_PRIVATE);

		String command = intent.getStringExtra(Rufius.command);

		String trigpath = unitPreferences.getString(Rufius.trigpath,null);
		if(trigpath==null)
		{
			trigpath = "/opt/rufius/triggers/";
		}
		String snappath = unitPreferences.getString(Rufius.snappath, null);
		if(snappath==null)
		{
			snappath = "/opt/rufius/snapshots/";
		}
		String downloadImage = intent.getStringExtra(Rufius.snappath);
		if(downloadImage==null)
		{
			downloadImage = trigpath + intent.getStringExtra(Rufius.trigpath);
		}
		else
		{
			downloadImage = snappath+downloadImage;
		}

		boolean downloadImageList = intent.getBooleanExtra(Rufius.desc, false);

		boolean key_check = intent.getBooleanExtra(Rufius.key, false);
		String unitPassword = intent.getStringExtra(Rufius.auth);
		boolean ipRetrieved = intent.getBooleanExtra(Rufius.rt_ip, false);
		boolean informOnError = intent.getBooleanExtra(Rufius.auto, true);

		String fingerprint = null;

		String gmail = unitPreferences.getString(Rufius.user_gmail, null);

		if((gmail==null) || (gmail.isEmpty()))
		{
			gmail = commonPreferences.getString(Rufius.user_gmail,null);
		}
		String serverEmail = unitPreferences.getString(Rufius.email,"");

		String dateString = unitPreferences.getString(Rufius.ip_date,"");

		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

		int error_code = unitPreferences.getInt(Rufius.error_code, 0x0000ffff);
		int initialStatus = unitPreferences.getInt(Rufius.status,0xfff0000);

		if((error_code & 0x0000000f)==0)
		{
			commanderStatus = 0x7ff0000;

			Intent intentBusy = new Intent(MainActivityReceiver.SET_BUSY);
			intentBusy.putExtra(Rufius.busy, true);
			lbm.sendBroadcast(intentBusy);

			unitPreferences.edit().putBoolean(Rufius.busy,true).apply();

			String known_hosts = this.getApplicationInfo().dataDir + "/files/known_hosts/"+unitFile;
			File known_hosts_file = new File(known_hosts);
			boolean knownHostsFileOK = true;

			if(!known_hosts_file.exists())
			{
				knownHostsFileOK = false;
				try
				{
					knownHostsFileOK = known_hosts_file.createNewFile();
				}
				catch(IOException xcpt)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("File known_hosts Creation Error: " + xcpt.getMessage());
					}
				}
			}

			if(knownHostsFileOK)
			{
				commanderStatus = 0x3ff0000;

				String authentication = "";
				boolean usePassword = true;

				if(unitPreferences.getBoolean(Rufius.auth,true))
				{
					usePassword = false;
					authentication = unitPreferences.getString(Rufius.key,"");
				}
				else if(unitPassword!=null)
				{
					authentication = unitPassword;
				}

				if(authentication!=null && (!authentication.isEmpty()))
				{
					commanderStatus = 0x1ff0000;

					String username = unitPreferences.getString(Rufius.user, "");
					int port = unitPreferences.getInt(Rufius.port_n, -1);
					String ip = null;

					if(unitPreferences.getBoolean(Rufius.statik,false))
					{
						ip = unitPreferences.getString(Rufius.st_ip,null);
					}
					else
					{
						String net = Rufius.getSSIDBSSID(this);
						if((net!=null) && (!net.isEmpty()) && (net.equals(unitFile)))
						{
							ip = unitPreferences.getString(Rufius.in_ip,null);
						}
					}

					if(ip==null || (ip.isEmpty()))
					{
						ip = unitPreferences.getString(Rufius.rt_ip,null);
					}

					if((ip!=null) && (!ip.isEmpty()))
					{
						Commander commander = new Commander();
						boolean commanderInitPrepareOK = false;

						if(usePassword)
						{
							commanderInitPrepareOK = commander.initiate(known_hosts) && commander.prepare(key_check,username,ip,port,authentication);
						}
						else
						{
							commanderInitPrepareOK = commander.initiate(known_hosts,authentication)&&commander.prepare(key_check,username,ip,port);
						}

						if(commanderInitPrepareOK)
						{
							if((command!=null))
							{
								if((command.equals(Rufius.COM_TOGGLE) || command.equals(Rufius.COM_STATUS)
										|| command.equals(Rufius.COM_INFENCE) || command.equals(Rufius.COM_OUTFENCE)
										|| command.equals(Rufius.COM_INWIFI) || command.equals(Rufius.COM_OUTWIFI)))
								{
									if(commander.connect())
									{
										commander.execute(command,true);
									}
									else
									{
										fingerprint = commander.getFingerPrint();

										if(fingerprint!=null&&(!fingerprint.isEmpty()) && (unitPreferences.getBoolean(Rufius.host,false))
												&& (fingerprint.equals(unitPreferences.getString(Rufius.fingerprint,""))) && (commander.prepare(true,username,ip,port))
												&& (commander.connect()))
										{
											commander.execute(command,true);
										}
									}
								}
							}
							else if(downloadImageList)
							{
								if(commander.connect())
								{
									Rufius.logDebug("Downloading Image List");
									commander.download(snappath+"list", this.getFilesDir().getAbsolutePath() + "/snapshots/" + unitFile + ".snapshots",false);
									commander.download(trigpath+"list",this.getFilesDir().getAbsolutePath()+"/triggers/"+unitFile+".triggers",true);
								}
							}
							else if(downloadImage!=null)
							{
								if(commander.connect())
								{
									Rufius.logDebug("Downloading Image");
									commander.download(downloadImage,this.getFilesDir().getAbsolutePath()+"/snapshots/"+unitFile+".jpg",true);
								}
							}
						}
						commanderStatus = commander.getStatus();
					}
				}
			}

			unitPreferences.edit().putBoolean(Rufius.busy,false).apply();

			intentBusy.putExtra(Rufius.busy, false);
			lbm.sendBroadcast(intentBusy);

			if((commanderStatus==0x1ff0000 || commanderStatus==0xf0000) && (gmail!=null) && (!gmail.isEmpty()) && !ipRetrieved)
			{
				Intent intentOut = new Intent(this,GoogleWorkerService.class);
				intentOut.putExtra(Rufius.user_gmail,gmail);
				intentOut.putExtra(Rufius.email,serverEmail);
				intentOut.putExtra(Rufius.unit_code,unitFile);
				intentOut.putExtra(Rufius.ip_date,dateString);
				intentOut.putExtra(Rufius.command,command);
				intentOut.putExtra(Rufius.auto,informOnError);
				this.startService(intentOut);
			}
		}

		if(command!=null)
		{
			unitPreferences.edit().putInt(Rufius.status,commanderStatus).apply();
		}

		String msg = unitPreferences.getString(Rufius.unit,"")+"\n"+ Rufius.createStatusMessage(commanderStatus);

		if(Rufius.isVisible())
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logDebug("Notifying through Activity");
			}

			Intent intentOut = null;

			if(command!=null)
			{
				Bundle bundle = new Bundle();
				bundle.putString(Rufius.unit_code, unitFile);

				if((commanderStatus==0x3ff0000))
				{
					if(informOnError)
					{
						intentOut = new Intent(MainActivityReceiver.SHOW_DIALOG);
					}
					else
					{
						intentOut = new Intent(MainActivityReceiver.SET_LIST_BUNDLE);
					}

					bundle.putBoolean(Rufius.auth, true);
					bundle.putString(Rufius.command, command);
				}
				else if((commanderStatus==0x70000) && (fingerprint!=null) && (!fingerprint.isEmpty()))
				{
					if(informOnError)
					{
						intentOut = new Intent(MainActivityReceiver.SHOW_DIALOG);
					}
					else
					{
						intentOut = new Intent(MainActivityReceiver.SET_LIST_BUNDLE);
					}

					bundle.putString(Rufius.fingerprint, fingerprint);
					bundle.putString(Rufius.command, command);
				}
				else
				{
					intentOut = new Intent(MainActivityReceiver.SHOW_MESSAGE);
					bundle.putString(Rufius.desc, msg);
				}

				intentOut.putExtra(Rufius.unit, bundle);
			}
			else if(downloadImageList)
			{
				if(commanderStatus==0x80)
				{
					intentOut = new Intent(UnitActivityReceiver.SET_IMAGE_LISTS);
				}
				else
				{
					intentOut = new Intent(UnitActivityReceiver.LIST_FAILURE);
				}
			}
			else
			{
				if(commanderStatus==0x80)
				{
					intentOut = new Intent(UnitActivityReceiver.SET_IMAGE);
				}
				else
				{
					intentOut = new Intent(UnitActivityReceiver.IMAGE_FAILURE);
				}
			}

			lbm.sendBroadcast(intentOut);
		}
		else if(((initialStatus!=commanderStatus)||((commanderStatus & 0xfff0000)!=0)))
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logDebug("Notifying through Statusbar");
			}

			Notification.Builder builder = new Notification.Builder(this);
			builder.setLights(0xffffffff, 1000, 1000);
			builder.setContentTitle("Rufius Info");
			if((commanderStatus&0xfff0000)!=0)
			{
				builder.setSmallIcon(R.drawable.ic_stat_notify_rv);
			}
			else if((commanderStatus==0x3ff0000) || (commanderStatus==0x70000))
			{
				builder.setSmallIcon(R.drawable.ic_stat_notify_q);
				builder.setContentInfo("Attention required!");
			}
			else
			{
				builder.setSmallIcon(R.drawable.ic_stat_notify);
			}

			builder.setAutoCancel(true);
			builder.setContentInfo(msg);

			Intent intentOut = new Intent(this, MainActivity.class);

			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			stackBuilder.addParentStack(MainActivity.class);
			stackBuilder.addNextIntent(intentOut);

			PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE)).notify(Rufius.NOTIFICATION_ID, builder.build());
		}

		this.stopSelf();
	}
}
