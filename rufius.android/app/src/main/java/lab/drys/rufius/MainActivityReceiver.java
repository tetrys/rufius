package lab.drys.rufius;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import lab.drys.rufius.utilities.DialogPermission;

/**
 * Created by lykanthrop on 6/5/15.
 */
public class MainActivityReceiver extends BroadcastReceiver
{
	public MainActivityReceiver()
	{
		super();
	}

	public MainActivityReceiver(MainActivity rfs)
	{
		activity = rfs;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String act = intent.getAction();

		if(act.equals(MainActivityReceiver.SHOW_MESSAGE))
		{
			Bundle bundle = intent.getBundleExtra(Rufius.unit);
			if(bundle!=null)
			{
				String msg = bundle.getString(Rufius.desc);
				if(msg!=null)
				{
					activity.showToastInfo(msg);
				}
			}
		}
		else if(act.equals(MainActivityReceiver.SHOW_DIALOG))
		{
			DialogPermission dlh = new DialogPermission();
			Bundle bundle = intent.getBundleExtra(Rufius.unit);
			if(bundle!=null)
			{
				dlh.setArguments(bundle);
				dlh.show(activity.getFragmentManager(), "");
			}
		}
		else if(act.equals(MainActivityReceiver.SET_BUSY))
		{
			activity.setBusy(intent.getBooleanExtra(Rufius.busy, false));
			activity.updateStatus();
		}
		else if(act.equals(MainActivityReceiver.UPDATE_STATUS))
		{
			activity.updateStatus();
		}
		else if(act.equals(MainActivityReceiver.GOOGLE_PERMISSION))
		{
			activity.setPendingIntentOnOK((Intent)intent.getParcelableExtra(Rufius.unit));
			activity.handleRecoverableAuthException((Intent)intent.getParcelableExtra(Rufius.desc));
		}
		else if(act.equals(MainActivityReceiver.UPDATE_LIST_STATUS))
		{
			activity.spotCheck();
			activity.updateStatus();
		}
		else if(act.equals(MainActivityReceiver.UPDATE_LIST_VIEW))
		{
			activity.updateList();
			activity.spotCheck();
			activity.updateStatus();
		}
		else if(act.equals(MainActivityReceiver.SET_LIST_BUNDLE))
		{
			if(intent!=null)
			{
				Bundle bundle = intent.getBundleExtra(Rufius.unit);
				if(bundle!=null)
				{
					activity.setListItemBundle(bundle.getString(Rufius.unit_code),bundle);
				}
			}
		}
		else if(act.equals(MainActivityReceiver.GOOGLE_IMAGE))
		{
			String str = intent.getStringExtra(Rufius.user);
			activity.setGoogleInfo(str);
		}
		else if(act.equals(MainActivityReceiver.RESET_COMMON))
		{
			activity.clearCommonPreferences();
		}
		else if(act.equals(MainActivityReceiver.DIALOG_CLOSED))
		{
			if(intent.getBooleanExtra(Rufius.status,false))
			{
				activity.endActionMode();
			}
		}
	}

	//Variables
	private MainActivity activity;

	//Actions
	public static final String SET_BUSY = "lab.drys.rufius.Rufius.Busy";
	public static final String UPDATE_STATUS = "lab.drys.rufius.Rufius.Status";
	public static final String GOOGLE_PERMISSION = "lab.drys.rufius.Rufius.GooglePermission";
	public static final String SHOW_MESSAGE = "lab.drys.rufius.Rufius.ShowMessage";
	public static final String SHOW_DIALOG = "lab.drys.rufius.Rufius.ShowDialog";
	public static final String UPDATE_LIST_STATUS = "lab.drys.rufius.Rufius.ListStatus";
	public static final String UPDATE_LIST_VIEW = "lab.drys.rufius.Rufius.ListView";
	public static final String SET_LIST_BUNDLE = "lab.drys.rufius.Rufius.ListBundle";
	public static final String GOOGLE_IMAGE = "lab.drys.rufius.Rufius.GoogleImage";
	public static final String RESET_COMMON = "lab.drys.rufius.Rufius.ResetCommon";
	public static final String DIALOG_CLOSED = "lab.drys.rufius.Rufius.DialogClosed";

}
