/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab.drys.rufius.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import lab.drys.rufius.Rufius;
import lab.drys.rufius.services.SwitcherService;

public class NetworkSwitchReceiver extends BroadcastReceiver
{
	public NetworkSwitchReceiver()
	{
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if(SwitcherService.isAvailable())
		{
			Intent intentOut = new Intent(context,SwitcherService.class);
			context.startService(intentOut);
		}
	}
}
