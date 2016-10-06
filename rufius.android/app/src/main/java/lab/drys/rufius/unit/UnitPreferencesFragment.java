/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab.drys.rufius.unit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import lab.drys.rufius.Rufius;

public class UnitPreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String preferenceFile = this.getArguments().getString(Rufius.unit_code);
		this.unitPreferences = this.getActivity().getSharedPreferences(preferenceFile, Context.MODE_PRIVATE);

		this.getPreferenceManager().setSharedPreferencesName(preferenceFile);
		this.addPreferencesFromResource(lab.drys.rufius.R.xml.unit_preferences);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		this.findPreference(Rufius.email).setEnabled(!this.unitPreferences.getBoolean(Rufius.statik, false));
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences unitPreferences, String key)
	{
		int out = unitPreferences.getInt(Rufius.error_code, 0);

		if(key.equals(Rufius.user))
		{
			String user = unitPreferences.getString(Rufius.user,"");
			if((user==null) || (user.isEmpty()))
			{
				out = out | Rufius.ERROR_USER;
			}
			else
			{
				out = out & Rufius.OK_USER;
			}
		}
		else if(key.equals(Rufius.port))
		{
			out = out | Rufius.ERROR_PORT & Rufius.OK_PORT_PRTFRM;
			int port = -1;
			String portString = unitPreferences.getString(Rufius.port,"");
			if((portString!=null) && (!portString.isEmpty()))
			{
				try
				{
					port = Integer.parseInt(portString);
				}
				catch(NumberFormatException xcpt)
				{
					Rufius.logError("Number Parsing Error: " + xcpt.getMessage());
				}

			}

			if(port<0 || port >0xffff)
			{
				out = out | Rufius.ERROR_PORT | Rufius.ERROR_PORT_PRTFRM;
			}
			else
			{
				out = out & Rufius.OK_PORT  & Rufius.OK_PORT_PRTFRM;;
			}

			unitPreferences.edit().putInt(Rufius.port_n,port).apply();
		}
		else if(key.equals(Rufius.auth))
		{
			boolean kth = unitPreferences.getBoolean(Rufius.auth, true);

			if(kth)
			{
				if((out & (Rufius.ERROR_AUTH_KEYMPT| Rufius.ERROR_AUTH_KEYNFN| Rufius.ERROR_AUTH_KEYNRK))!=0)
				{
					out = out | Rufius.ERROR_AUTH;
				}
				else
				{
					out = out & Rufius.OK_AUTH;
				}
			}
			else
			{
				out = out & Rufius.OK_AUTH;
			}
		}
		else if(key.equals(Rufius.key))
		{
			String filePath = unitPreferences.getString(Rufius.key,"");
			if((filePath==null) || (filePath.isEmpty()))
			{
				out = out | Rufius.ERROR_AUTH | Rufius.ERROR_AUTH_KEYMPT;
			}
			else
			{
				out = out & Rufius.OK_AUTH_KEYMPT;

				File file = new File(filePath);
				if(!file.exists())
				{
					out = out | Rufius.ERROR_AUTH | Rufius.ERROR_AUTH_KEYNFN;
				}
				else
				{
					out = out & Rufius.OK_AUTH_KEYNFN;

					try
					{
						BufferedReader bfrd = new BufferedReader(new FileReader(file));
						String head = bfrd.readLine();
						if(head!=null)
						{
							head.replace("\n","");
						}

						if(head==null || (!head.equals(Rufius.RSA_FILE_HEADER)))
						{
							out = out | Rufius.ERROR_AUTH | Rufius.ERROR_AUTH_KEYNRK;
						}
						else
						{

							out = out & Rufius.OK_AUTH & Rufius.OK_AUTH_KEYNRK;
						}
					}
					catch(IOException xcpt)
					{
						out = out | Rufius.ERROR_AUTH | Rufius.ERROR_AUTH_KEYNRK;
						Rufius.logError("Key File Reading Error: " + xcpt.getMessage());
					}
				}
			}
		}
		else if(key.equals(Rufius.statik))
		{
			boolean st = unitPreferences.getBoolean(Rufius.statik,false);
			this.findPreference(Rufius.email).setEnabled(!st);

			if(st)
			{
				if((out & (Rufius.ERROR_INPR_SSTMPT| Rufius.ERROR_INPR_SSTFRM| Rufius.ERROR_INPR_SSTNDM))!=0)
				{
					out = out | Rufius.ERROR_INPR;
				}
				else
				{
					out = out & Rufius.OK_INPR;
				}
			}
			else
			{
				if((out & (Rufius.ERROR_INPR_SDNNEM| Rufius.ERROR_INPR_SDNMFR))!=0)
				{
					out = out | Rufius.ERROR_INPR;
				}
				else
				{
					out = out & Rufius.OK_INPR;
				}
			}
		}
		else if(key.equals(Rufius.in_ip))
		{
			String statikIP = unitPreferences.getString(Rufius.st_ip,"");
			if((statikIP==null) || (statikIP.isEmpty()))
			{
				out = out | Rufius.ERROR_INPR_SNTMPT;
			}
			else
			{
				out = out & Rufius.OK_INPR_SNTMPT;

				if(!statikIP.matches(Rufius.regex_IP))
				{
					out = out | Rufius.ERROR_INPR_SNTFRM;
				}
				else
				{
					out = out & Rufius.OK_INPR_SNTFRM;

					if(!(statikIP.matches(Rufius.regex_intraIP_16)||statikIP.matches(Rufius.regex_intraIP_20)||statikIP.matches(Rufius.regex_intraIP_24)))
					{
						out = out | Rufius.ERROR_INPR_SNTNNT;
					}
					else
					{
						out = out & Rufius.OK_INPR_SNTNNT;
					}
				}
			}
		}
		else if(key.equals(Rufius.st_ip))
		{
			String statikIP = unitPreferences.getString(Rufius.st_ip,"");
			if((statikIP==null) || (statikIP.isEmpty()))
			{
				out = out | Rufius.ERROR_INPR | Rufius.ERROR_INPR_SSTMPT;
			}
			else
			{
				out = out & Rufius.OK_INPR_SSTMPT;

				if(!statikIP.matches(Rufius.regex_IP))
				{
					out = out | Rufius.ERROR_INPR | Rufius.ERROR_INPR_SSTFRM;
				}
				else
				{
					out = out & Rufius.OK_INPR_SSTFRM;

					if(statikIP.matches(Rufius.regex_intraIP_16)||statikIP.matches(Rufius.regex_intraIP_20)||statikIP.matches(Rufius.regex_intraIP_24))
					{
						out = out | Rufius.ERROR_INPR | Rufius.ERROR_INPR_SSTNDM;
					}
					else
					{
						out = out & Rufius.OK_INPR & Rufius.OK_INPR_SSTNDM;
					}
				}
			}
		}
		else if(key.equals(Rufius.email))
		{
			String serverEmail = unitPreferences.getString(Rufius.email,"");
			if((serverEmail==null) || (serverEmail.isEmpty()))
			{
				out = out | Rufius.ERROR_INPR | Rufius.ERROR_INPR_SDNNEM;
			}
			else
			{
				out = out & Rufius.OK_INPR_SDNNEM;

				if(!serverEmail.matches(Rufius.regex_email))
				{
					out = out | Rufius.ERROR_INPR | Rufius.ERROR_INPR_SDNMFR;
				}
				else
				{
					out = out & Rufius.OK_INPR & Rufius.OK_INPR_SDNMFR;
				}
			}
		}

		unitPreferences.edit().putInt(Rufius.error_code,out).apply();
	}

	SharedPreferences unitPreferences;
}
