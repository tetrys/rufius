package lab.drys.rufius.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

import lab.drys.rufius.MainActivityReceiver;
import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;
import lab.drys.rufius.services.MainService;

/**
 * Created by lykanthrop on 6/5/15.
 */
public class DialogUnitDeletion extends DialogFragment implements DialogInterface.OnClickListener
{
	public DialogUnitDeletion()
	{
		super();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		toDeletion = (ArrayList<String>)this.getArguments().getSerializable(Rufius.unit);

		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setIcon(R.drawable.ic_warning_black_48dp);
		builder.setTitle(R.string.mssg_delete);
		builder.setMessage("Delete the Selected Units?");
		builder.setPositiveButton(R.string.dlg_yes, this);
		builder.setNegativeButton(R.string.dlg_no, this);

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int id)
	{
		Intent intent;
		switch(id)
		{
			case DialogInterface.BUTTON_POSITIVE:

				intent = new Intent(this.getActivity(), MainService.class);
				intent.putExtra(Rufius.unit,toDeletion);
				intent.putExtra(Rufius.ready,true);
				this.getActivity().startService(intent);

				intent = new Intent(MainActivityReceiver.DIALOG_CLOSED);
				intent.putExtra(Rufius.status,true);
				LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);

				break;
			case DialogInterface.BUTTON_NEGATIVE:

				intent = new Intent(MainActivityReceiver.DIALOG_CLOSED);
				intent.putExtra(Rufius.status,false);
				LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);

				break;
			default:
				break;
		}
	}

	//Variables
	ArrayList<String>  toDeletion;
}
