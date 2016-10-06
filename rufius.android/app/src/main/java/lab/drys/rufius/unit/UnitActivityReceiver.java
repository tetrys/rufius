package lab.drys.rufius.unit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by lykanthrop on 1507/15/.
 */
public class UnitActivityReceiver extends BroadcastReceiver
{
	public UnitActivityReceiver()
	{
		super();
	}

	public UnitActivityReceiver(UnitActivity rfs)
	{
		activity = rfs;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String act = intent.getAction();

		if(act.equals(SET_IMAGE))
		{
			activity.getImage();
			activity.setImageBusy(false);
		}
		else if(act.equals(SET_IMAGE_LISTS))
		{
			activity.parseLists();
			activity.setBusy(false);
		}
		else if(act.equals(UPDATE_IMAGE_LISTS))
		{
			activity.updateLists();
			activity.downloadSnapshot("lastsnap.jpg");
		}
		else if(act.equals(LIST_FAILURE))
		{
			activity.setBusy(false);
		}
		else if(act.equals(IMAGE_FAILURE))
		{
			activity.setImageBusy(false);
		}
	}

	//Variable
	private UnitActivity activity;

	public static final String SET_IMAGE = "lab.drys.rufius.Unit.Image";
	public static final String SET_IMAGE_LISTS = "lab.drys.rufius.Unit.SetLists";
	public static final String UPDATE_IMAGE_LISTS = "lab.drys.rufius.Unit.UpdateLists";
	public static final String IMAGE_FAILURE = "lab.drys.rufius.Unit.ImageFailure";
	public static final String LIST_FAILURE = "lab.drys.rufius.Unit.ListFailure";
}
