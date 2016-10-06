package lab.drys.rufius;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;

/**
 * Created by lykanthrop on 5/27/15.
 */
public class Rufius extends Application
{
	public static boolean isVisible()
	{
		return mainActivityVisible || preferenceActivityVisible || unitActivityVisible;
	}

	public static boolean isMainActivityVisible()
	{
		return mainActivityVisible;
	}

	public static boolean isUnitActivityVisible()
	{
		return unitActivityVisible;
	}

	public static boolean isPreferenceActivityVisible()
	{
		return preferenceActivityVisible;
	}

	public static void mainActivityResumed()
	{
		mainActivityVisible = true;
	}

	public static void mainActivityPaused()
	{
		mainActivityVisible = false;
	}

	public static void unitActivityResumed()
	{
		unitActivityVisible = true;
	}

	public static void unitActivityPaused()
	{
		unitActivityVisible = false;
	}

	public static void preferenceActivityResumed()
	{
		preferenceActivityVisible = true;
	}

	public static void preferenceActivityPaused()
	{
		preferenceActivityVisible = false;
	}

	public static String generateErrorInfo(int error_code, boolean statikIP)
	{
		String out = "";

		if((error_code & ERROR_USER)!=0)
		{
			out+="Username is Missing\n";
		}

		if((error_code & ERROR_PORT_PRTFRM)!=0)
		{
			out+="Port out of Range\n";
		}
		else if((error_code & ERROR_PORT)!=0)
		{
			out+="Port is Missing\n";
		}

		if((error_code & ERROR_AUTH)!=0)
		{
			if((error_code & ERROR_AUTH_KEYNFN)!=0)
			{
				out+="Key File not Found\n";
			}
			else if((error_code & ERROR_AUTH_KEYNRK)!=0)
			{
				out+="Key File seems Broken\n";
			}
			else
			{
				out+="Path to Key is Missing\n";
			}
		}

		if((error_code & ERROR_INPR)!=0)
		{
			if(statikIP)
			{
				if((error_code & ERROR_INPR_SSTFRM)!=0)
				{
					out+="Global Static IP Illegal Format\n";
				}
				else if((error_code & ERROR_INPR_SSTNDM)!=0)
				{
					out+="Global Static IP seems Local\n";
				}
				else
				{
					out+="Global Static IP is Missing\n";
				}
			}
			else
			{

				if((error_code & ERROR_INPR_SDNMFR)!=0)
				{
					out+="Server Email Illegal Format\n";
				}
				else
				{
					out+="Server Email is Missing\n";
				}
			}
		}

		if(!out.isEmpty())
		{
			out = out.substring(0,out.length()-2);
		}

		return out;
	}

	public static int getGraceTime(String gt)
	{
		int out = 0;
		try
		{
			out = Integer.parseInt(gt);
		}
		catch(NumberFormatException xcpt)
		{
			Rufius.logWarning("Grace Time Error: " + xcpt.getMessage());
		}

		return out;
	}

	public static int getStatusResourceId(int i)
	{
		int out;

		if((i==0x3ff0000) || (i==0x70000))
		{
			out = R.drawable.ic_report_black_48dp;
		}
		else if((i&0xffff0000)!=0)
		{
			out = R.drawable.ic_warning_black_48dp;
		}
		else if(i==0)
		{
			out = R.drawable.ic_error_black_48dp;
		}
		else if((i&0x09)==0x09)
		{
			out = R.drawable.ic_visibility_black_48dp;
		}
		else if((i&0x09)==0x01)
		{
			out = R.drawable.ic_visibility_off_black_48dp;
		}
		else
		{
			out = R.drawable.ic_warning_black_48dp;
		}

		return out;
	}

	public static String generateInfo(SharedPreferences unit)
	{
		String out = "Guard : ";

		out+=unit.getString(Rufius.unit,"")+"\n";

		String str;
		if(!(str = unit.getString(Rufius.desc,"")+"\n").isEmpty())
		{
			out+=str;
		}
		out+="Username : "+unit.getString(Rufius.user,"")+"\n";

		if(!(str = unit.getString(Rufius.in_ip,"")).isEmpty())
		{
			out+="Local Static IP : "+str+"\n";
		}
		if(unit.getBoolean(Rufius.statik,false) && (!(str = unit.getString(Rufius.st_ip,"")).isEmpty()))
		{
			out+="Global Static IP : "+str+"\n";
		}
		else if(!(str = unit.getString(Rufius.rt_ip,"")).isEmpty())
		{
			out+="Global IP : "+str+"\n";
		}

		out+=Rufius.createStatusMessage(unit.getInt(Rufius.status,0xfff0000));

		return out;
	}

	public static String createStatusMessage(int commanderStatus)
	{
		String msg;
		if(commanderStatus == 0xfff0000)
		{
			msg = "Configuration Error!";
		}
		else if(commanderStatus == 0x7ff0000)
		{
			msg = "Known Hosts File Error!";
		}
		else if(commanderStatus == 0x3ff0000)
		{
			msg = "Authentication Error!";
		}
		else if(commanderStatus == 0x1ff0000)
		{
			msg = "IP Error!";
		}
		else if(commanderStatus == 0x0ff0000)
		{
			msg = "Fetched IP Error!";
		}
		else if(commanderStatus == 0x07f0000)
		{
			msg = "SSh Channel Error!";
		}
		else if(commanderStatus == 0x03f0000)
		{
			msg = "SSh Session Error!";
		}
		else if(commanderStatus == 0x01f0000)
		{
			msg = "SSh Connection Error!";
		}
		else if(commanderStatus == 0x00f0000)
		{
			msg = "Trying to get new IP!";
		}
		else if(commanderStatus == 0x0070000)
		{
			msg = "";
		}
		else if(commanderStatus==0x0030000)
		{
			msg = "SSh Execution Error!";
		}
		else if(commanderStatus==0x0010000)
		{
			msg = "SSh Channel Input Error!";
		}
		else if(commanderStatus==0x0)
		{
			msg = "Server Down";
		}
		else if((commanderStatus&0x0f)!=0)
		{
			msg = "";
			if((commanderStatus&0x01)!=0)
			{
				msg+="Server Up";
			}
			else
			{
				msg+="Server Down";
			}

			if((commanderStatus&0x08)!=0)
			{
				msg+="\nDetect On";
			}
			else
			{
				msg+="\nDetect Off";
			}

			if((commanderStatus&0x04)!=0)
			{
				msg+="\nMonitor On";
			}
			else
			{
				msg+="\nMonitor Off";
			}

			if((commanderStatus&0x400)!=0)
			{
				msg+="\nInfo On";
			}
			else
			{
				msg+="\nInfo Off";
			}

			if((commanderStatus&0x200)!=0)
			{
				msg += "\nYou are a Superuser";
			}
		}
		else if(commanderStatus==0x80)
		{
			msg = "Snapshot Downloaded";
		}
		else
		{
			msg = "Unknown Error!";
		}

		return  msg;
	}

	public static String getSSIDBSSID(Context context)
	{
		String out = null;

		NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		if((activeNetwork != null) && (activeNetwork.isConnectedOrConnecting()))
		{
			out = "";
			if((activeNetwork.getType() == ConnectivityManager.TYPE_WIFI))
			{
				WifiInfo winf = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
				if(winf != null)
				{
					out = winf.getSSID().replaceAll("\"", "")+winf.getBSSID().replaceAll(":","");
				}
			}
		}

		return out;
	}

	public static boolean isIPIntra(String ip)
	{
		return ip.matches(regex_IP) && (ip.matches(regex_intraIP_24) || ip.matches(regex_intraIP_20) || ip.matches(regex_intraIP_16));
	}

	public static boolean isIPInter(String ip)
	{
		return  ip.matches(regex_IP) && (!(ip.matches(regex_intraIP_24) || ip.matches(regex_intraIP_20) || ip.matches(regex_intraIP_16)));
	}

	public static void logDebug(String str)
	{
		Log.d("Rufius",str);
	}

	public static void logInfo(String str)
	{
		Log.i("Rufius", str);
	}

	public static void logWarning(String str)
	{
		Log.w("Rufius", str);
	}

	public static void logError(String str)
	{
		Log.e("Rufius", str);
	}

	//Variables
	private static boolean mainActivityVisible;
	private static boolean preferenceActivityVisible;
	private static boolean unitActivityVisible;

	public static final boolean RELEASE_FLAG = false;

	public static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
	public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;
	public static final int REQUEST_CODE_CREATE_NEW_UNIT = 1002;
	public static final int REQUEST_CODE_EDIT_UNIT = 1003;
	public static final String SCOPE = "oauth2:";
	public static final String SCOPE_GMAIL = "https://mail.google.com";
	public static final String SCOPE_PROFILE = "profile";

	//Unit Editable Preferences
	public static final String unit = "unit"; //bundle
	public static final String desc = "desc"; //message
	public static final String user = "user";
	public static final String auth = "auth"; //password
	public static final String key = "key";
	public static final String in_ip = "in_ip";
	public static final String port = "port";
	public static final String statik = "statik"; //common preferences file path
	public static final String st_ip = "st_ip";
	public static final String email = "email";
	public static final String sync = "sync"; // units list status refresh
	public static final String auto = "auto"; // units list
	public static final String time = "time";

	//Unit Non-editable Preferences
	public static final String unit_code = "unit_code"; //String //current network //unit file path
	public static final String user_gmail = "user_gmail"; //String
	public static final String fingerprint = "fingerprint"; //String
	public static final String rt_ip = "rt_ip"; //String
	public static final String ip_date = "ip_date"; //String
	public static final String status = "status"; //int
	public static final String error_code = "error_code"; //int
	public static final String port_n = "port_n"; //int
	public static final String busy = "busy"; //boolean
	public static final String ready = "ready"; //boolean //flag
	public static final String host = "host"; //boolean
	public static final String snappath = "snappath"; //String
	public static final String trigpath = "trigpath"; //String

	//Regex
	public static final String regex_0_255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	public static final String regex_16_31 = "(?:1[6-9]|2[0-9]|3[0-1])";
	public static final String regex_IP = regex_0_255+"\\."+regex_0_255+"\\."+regex_0_255+"\\."+regex_0_255;
	public static final String regex_intraIP_24 = "10\\."+regex_0_255+"\\."+regex_0_255+"\\."+regex_0_255;
	public static final String regex_intraIP_16 = "192\\.168\\."+regex_0_255+"\\."+regex_0_255;
	public static final String regex_intraIP_20 = "17\\."+regex_16_31+"\\."+regex_0_255+"\\."+regex_0_255;
	public static final String regex_dns = "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])";
	public static final String regex_email = "[a-zA-Z](?:(?:\\.|_)?[a-zA-Z0-9])*[a-zA-Z0-9]@[a-zA-Z][a-zA-Z0-9](?:\\.?[a-zA-Z0-9])*\\.[a-zA-Z]{3,}";
	public static final String regex_mac_address = "(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}";

	public static final String RSA_FILE_HEADER = "-----BEGIN RSA PRIVATE KEY-----";

	//Rufius Commands
	public static final String command = "command";
	public static final String RUFIUS_COM = "/opt/rufius/rufius -m";
	public static final String COM_INIT = RUFIUS_COM + " init";
	public static final String COM_TOGGLE = RUFIUS_COM + " toggle";
	public static final String COM_OUTFENCE = RUFIUS_COM + " outfence";
	public static final String COM_INFENCE = RUFIUS_COM + " infence";
	public static final String COM_OUTWIFI = RUFIUS_COM + " outwifi";
	public static final String COM_INWIFI = RUFIUS_COM + " inwifi";
	public static final String COM_INFOFF = RUFIUS_COM + " infoff";
	public static final String COM_INFON = RUFIUS_COM + " infon";
	public static final String COM_DETOFF = RUFIUS_COM + " detoff";
	public static final String COM_DETON = RUFIUS_COM + " deton";
	public static final String COM_SNAPOFF = RUFIUS_COM + " snapoff";
	public static final String COM_SNAPON = RUFIUS_COM + " snapon";
	public static final String COM_STATUS = RUFIUS_COM + " status";

	public static final int ERROR_USER = 0x00000001;
	public static final int ERROR_PORT = 0x00000002;
	public static final int ERROR_AUTH = 0x00000004;
	public static final int ERROR_INPR = 0x00000008;
	public static final int ERROR_PORT_PRTFRM = 0x00000010;
	public static final int ERROR_AUTH_KEYMPT = 0x00000020;
	public static final int ERROR_AUTH_KEYNFN = 0x00000040;
	public static final int ERROR_AUTH_KEYNRK = 0x00000080;
	public static final int ERROR_INPR_SSTMPT = 0x00000100;
	public static final int ERROR_INPR_SSTFRM = 0x00000200;
	public static final int ERROR_INPR_SSTNDM = 0x00000400;
	public static final int ERROR_INPR_SDNNEM = 0x00000800;
	public static final int ERROR_INPR_SDNMFR = 0x00001000;
	public static final int ERROR_INPR_SNTMPT = 0x00002000;
	public static final int ERROR_INPR_SNTFRM = 0x00004000;
	public static final int ERROR_INPR_SNTNNT = 0x00008000;

	public static final int OK_USER = 0xfffffffe;
	public static final int OK_PORT = 0xfffffffd;
	public static final int OK_AUTH = 0xfffffffb;
	public static final int OK_INPR = 0xfffffff7;
	public static final int OK_PORT_PRTFRM = 0xffffffef;
	public static final int OK_AUTH_KEYMPT = 0xffffffdf;
	public static final int OK_AUTH_KEYNFN = 0xffffffbf;
	public static final int OK_AUTH_KEYNRK = 0xffffff7f;
	public static final int OK_INPR_SSTMPT = 0xfffffeff;
	public static final int OK_INPR_SSTFRM = 0xfffffdff;
	public static final int OK_INPR_SSTNDM = 0xfffffbff;
	public static final int OK_INPR_SDNNEM = 0xfffff7ff;
	public static final int OK_INPR_SDNMFR = 0xffffefff;
	public static final int OK_INPR_SNTMPT = 0xffffdfff;
	public static final int OK_INPR_SNTFRM = 0xffffbfff;
	public static final int OK_INPR_SNTNNT = 0xffff8fff;

	public static final int NOTIFICATION_ID = 1000;
}
