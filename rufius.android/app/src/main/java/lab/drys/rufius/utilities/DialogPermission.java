package lab.drys.rufius.utilities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import lab.drys.rufius.MainActivityReceiver;
import lab.drys.rufius.R;
import lab.drys.rufius.Rufius;
import lab.drys.rufius.services.SShService;

/**
 * Created by lykanthrop on 6/9/15.
 */
public class DialogPermission extends DialogFragment implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener
{
	public DialogPermission()
	{
		super();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Bundle bundle = this.getArguments();
		command = bundle.getString(Rufius.command);
		unitFilename = bundle.getString(Rufius.unit_code);
		switchPermission = !bundle.getBoolean(Rufius.auto, false);
		askPassword = bundle.getBoolean(Rufius.auth,false);
		fingerprint = bundle.getString(Rufius.fingerprint);
		statik = bundle.getBoolean(Rufius.statik,false);

		String title = "";

		if(unitFilename!=null)
		{
			unitPreferences = this.getActivity().getSharedPreferences(unitFilename, Context.MODE_PRIVATE);

			title = unitPreferences.getString(Rufius.unit,"");

			if((title!=null) && (!title.isEmpty()))
			{
				if(switchPermission)
				{
					title+=" requires Permission!";
				}
				else if(askPassword)
				{
					title+=" requires Password!";
				}
				else if(fingerprint!=null)
				{
					title+= " is an Unknown Host!";
				}
			}
			else
			{
				if(switchPermission)
				{
					title = "Permission Required!";
				}
				else if(askPassword)
				{
					title = "Password Required!";
				}
				else if(fingerprint!=null)
				{
					title = "Unknown Host!";
				}
			}
		}
		else if(statik)
		{
			title = "Reset!";
		}



		AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
		builder.setIcon(R.drawable.ic_warning_black_48dp);

		builder.setTitle(title);
		builder.setPositiveButton(R.string.dlg_yes, this);
		builder.setNegativeButton(R.string.dlg_no, this);

		if(askPassword)
		{
			View customView = this.getActivity().getLayoutInflater().inflate(R.layout.dialog_password,null);
			passField = (EditText)customView.findViewById(R.id.pass_dialog_field);

			builder.setView(customView);
		}
		else if(fingerprint!=null)
		{
			View customView = this.getActivity().getLayoutInflater().inflate(R.layout.dialog_unknown_host,null);

			TextView message = (TextView)customView.findViewById(R.id.dialog_message);
			CheckBox checkBox = (CheckBox)customView.findViewById(R.id.host_remember);
			checkBox.setOnCheckedChangeListener(this);
			message.setText("Server Fingerprint is.\n" + fingerprint + "\nAdd to Known Hosts?");

			builder.setView(customView);
		}
		else if(statik)
		{
			View customView = this.getActivity().getLayoutInflater().inflate(R.layout.dialog_message,null);

			TextView message = (TextView)customView.findViewById(R.id.dialog_message);
			message.setText("Reset Common Settings?");

			builder.setView(customView);
		}

		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int id)
	{
		Intent intent;
		switch(id)
		{
			case DialogInterface.BUTTON_POSITIVE:

				if(statik)
				{
					intent = new Intent(MainActivityReceiver.RESET_COMMON);
					LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
				}
				else
				{
					intent = new Intent(this.getActivity(), SShService.class);
					intent.putExtra(Rufius.unit_code, unitFilename);
					intent.putExtra(Rufius.command, command);

					if(askPassword)
					{
						String password = passField.getText().toString();
						if((!password.isEmpty()))
						{
							intent.putExtra(Rufius.auth, password);
						}
					}
					else if(fingerprint!=null)
					{
						unitPreferences.edit()
								.putString(Rufius.fingerprint,fingerprint)
								.putBoolean(Rufius.host, selected).apply();

						intent.putExtra(Rufius.host, true);
					}

					this.getActivity().startService(intent);

					intent = new Intent(MainActivityReceiver.DIALOG_CLOSED);
					intent.putExtra(Rufius.status,true);
					LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
				}

				break;
			case DialogInterface.BUTTON_NEGATIVE:
				intent = new Intent(MainActivityReceiver.DIALOG_CLOSED);
				intent.putExtra(Rufius.status,false);
				LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
				this.dismiss();
				break;
			default:
				break;
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton button, boolean isChekced)
	{
		if(isChekced)
		{
			selected = true;
		}
		else if(selected)
		{
			selected = false;
		}
	}

	//Variables
	private EditText passField;
	private String command;
	private String unitFilename;
	private boolean switchPermission;
	private boolean askPassword;
	private boolean statik;
	private String fingerprint;
	private boolean selected;
	private SharedPreferences unitPreferences;
}
