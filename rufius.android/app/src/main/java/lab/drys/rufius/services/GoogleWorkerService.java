package lab.drys.rufius.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import lab.drys.rufius.Rufius;
import lab.drys.rufius.MainActivityReceiver;

/**
 * Created by lykanthrop on 5/25/15.
 */
public class GoogleWorkerService extends IntentService
{
	public GoogleWorkerService()
	{
		super("GmailReaderService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent,flags,startId);

		return START_STICKY;
	}

	@Override
	public void onHandleIntent(Intent intent)
	{
		incomingIntent = intent;
		userEmail = intent.getStringExtra(Rufius.user_gmail);
		serverEmail = intent.getStringExtra(Rufius.email);
		unitFile = intent.getStringExtra(Rufius.unit_code);
		command = intent.getStringExtra(Rufius.command);
		oldDateString = intent.getStringExtra(Rufius.ip_date);
		user = intent.getBooleanExtra(Rufius.user, false);
		inforOnError = intent.getBooleanExtra(Rufius.auto,true);

		gmailService = null;

		if((!user) && (userEmail!=null))
		{
			this.init();

			if(gmailService!=null)
			{
				if(serverEmail!=null)
				{
					this.read();
				}
			}
		}
		else if(user)
		{
			this.getProfileImage();
		}
	}

	private void init()
	{
		String token = null;

		try
		{
			token = fetchToken(this, Rufius.SCOPE_GMAIL);
		}
		catch(IOException xcpt)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logError("Fetcher Exception: " + xcpt.getMessage());
			}
		}

		if(token != null)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logError("");
			}

			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();
			GoogleTokenResponse response = new GoogleTokenResponse();
			response.setAccessToken(token);
			GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);
			gmailService = new Gmail.Builder(httpTransport, jsonFactory, credential).setApplicationName("Rufius").build();
		}
		else
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logDebug("Token is Null");
			}
			try
			{
				java.lang.Thread.sleep(10000);
			}
			catch(InterruptedException e)
			{

			}

		}
	}

	private void getProfileImage()
	{
		Rufius.logDebug("Fetching Profile");
		String token = null;

		try
		{
			token = fetchToken(this, Rufius.SCOPE_PROFILE);
		}
		catch(IOException xcpt)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logError("Fetcher Exception: " + xcpt.getMessage());
			}
		}

		if(token != null)
		{
			Rufius.logDebug("Token Not Null");
			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();
			GoogleTokenResponse response = new GoogleTokenResponse();
			response.setAccessToken(token);
			GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);
			Plus plus = new Plus.Builder(httpTransport,jsonFactory,credential).setApplicationName("Rufius").build();

			Person person = null;
			try
			{
				person = plus.people().get("me").execute();
				Rufius.logDebug("Image URL: " + person.getImage().getUrl());
			}
			catch(IOException xcpt)
			{
				Rufius.logError(xcpt.getMessage());
			}

			if(person!=null)
			{
				String imageURL = person.getImage().getUrl();
				String gmailName = person.getDisplayName();

				int ind = imageURL.indexOf("?");

				imageURL = imageURL.substring(0,ind);

				Rufius.logDebug(imageURL);

				Bitmap icon = null;
				try
				{
					InputStream in = new java.net.URL(imageURL).openStream();
					icon = BitmapFactory.decodeStream(in);
				}
				catch(IOException xcpt)
				{
					Rufius.logError(xcpt.getMessage());
				}

				if(icon!=null)
				{
					Bitmap output = Bitmap.createBitmap(icon.getWidth(),
							icon.getHeight(), Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(output);
					final Paint paint = new Paint();
					final Rect rect = new Rect(0, 0, icon.getWidth(),
							icon.getHeight());

					paint.setAntiAlias(true);
					canvas.drawARGB(0, 0, 0, 0);
					canvas.drawCircle(icon.getWidth() / 2,
							icon.getHeight() / 2, icon.getWidth() / 2, paint);
					paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
					canvas.drawBitmap(icon, rect, rect, paint);

					String filename = this.getApplicationInfo().dataDir+"/"+userEmail+".png";
					FileOutputStream out = null;
					boolean compOK = false;
					try
					{
						out = new FileOutputStream(filename);
						compOK = output.compress(Bitmap.CompressFormat.PNG, 100, out);
					}
					catch(FileNotFoundException xcpt)
					{
						Rufius.logError(xcpt.getMessage());
					}

					if(out!=null)
					{
						try
						{
							out.close();
						}
						catch(IOException xcpt)
						{
							Rufius.logError(xcpt.getMessage());
						}
					}

					if(compOK)
					{
						Intent intentOut = new Intent(MainActivityReceiver.GOOGLE_IMAGE);
						intentOut.putExtra(Rufius.user,gmailName);
						LocalBroadcastManager.getInstance(this).sendBroadcast(intentOut);
					}
				}
			}
		}
	}

	public void read()
	{
		if(gmailService!=null)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logDebug("Reading: " + serverEmail);
			}
			ListMessagesResponse messagesResponse = null;

			String query = "from: " + serverEmail + " subject: IPChange";

			try
			{
				messagesResponse = gmailService.users().messages().list(userEmail).setQ(query).execute();
			}
			catch(IOException xcpt)
			{
				Rufius.logError(xcpt.getMessage());
			}

			if(messagesResponse != null)
			{
				List<Message> msgs = messagesResponse.getMessages();


				if(msgs!=null)
				{
					SharedPreferences unitPreferences = this.getSharedPreferences(unitFile, MODE_PRIVATE);
					Message oldMessage = null;
					String info = null;
					boolean deleteMessage = true;
					String theIP = null;

					for(Message ms : msgs)
					{
						info = null;
						try
						{
							ms = gmailService.users().messages().get(userEmail,ms.getId()).execute();
							info = ms.getSnippet();
						}
						catch(IOException xcpt)
						{
							Rufius.logError("Fetching Error: " + xcpt.getMessage());
						}

						if(info!=null)
						{
							Rufius.logDebug("Snippet: " + info);

							int is = info.indexOf("&lt;");
							int ie = info.indexOf("&gt;");
							int im = info.indexOf(" ");

							if(is>-1 && ie >-1)
							{
								String strDate = info.substring(0,im);
								Rufius.logDebug(strDate);
								String unit = info.substring(is+4,ie);
								unit = unit.replaceAll("(@|:)","");
								Rufius.logDebug(unit);
								String ip = info.substring(ie+4);
								Rufius.logDebug(ip);

								if((unitFile!=null)&&(!unitFile.isEmpty())&& Rufius.isIPInter(ip)&&(unit.equals(unitFile)))
								{
									Rufius.logDebug("Retrieved Unit: " + unit);
									Rufius.logDebug("IP: " + ip);
									Rufius.logDebug("Date: " + strDate);

									SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
									Date date =null;
									Date oldDate = null;

									try
									{
										date = format.parse(strDate);

										if(oldDateString!=null)
										{
											oldDate = format.parse(oldDateString);
											Rufius.logDebug("Old Date"+oldDate.toString());
										}
									}
									catch(ParseException xcpt)
									{

									}

									if(oldDate==null || oldDate.before(date) || oldDate.equals(date))
									{
										Rufius.logDebug("New IP: "+ip);

										oldDateString = strDate;
										theIP = ip;
										deleteMessage = false;
									}
								}
							}
						}

						if(deleteMessage)
						{
							oldMessage = ms;
						}

						if(!Rufius.RELEASE_FLAG)
						{
							Rufius.logDebug("Message Id: " + ms.getId());
							Rufius.logDebug("Message Thread Id: " + ms.getThreadId());
						}

						if(oldMessage!=null)
						{
							try
							{
								gmailService.users().messages().trash(userEmail,oldMessage.getId()).execute();

								Rufius.logDebug("Trashed");
							}
							catch(IOException xcpt)
							{
								Rufius.logError("Trashing Error: " + xcpt.getMessage());
							}
						}

						oldMessage = ms;
					}

					unitPreferences.edit().putString(Rufius.rt_ip, theIP).putString(Rufius.ip_date,oldDateString).apply();

					if((command!=null)&&(!command.isEmpty()))
					{
						Intent intent = new Intent(this,SShService.class);
						intent.putExtra(Rufius.unit_code,unitFile);
						intent.putExtra(Rufius.command,command);
						intent.putExtra(Rufius.rt_ip,true);
						intent.putExtra(Rufius.auto,inforOnError);
						this.startService(intent);
					}
				}
			}
		}
	}

	protected String fetchToken(Context cntx, String scope) throws IOException
	{
		try
		{
			return GoogleAuthUtil.getToken(cntx,userEmail, Rufius.SCOPE+scope);
		}
		catch(UserRecoverableAuthException xcpt)
		{
			Intent intent = (Intent)incomingIntent.clone();

			Intent intentOut = new Intent(MainActivityReceiver.GOOGLE_PERMISSION);
			intentOut.putExtra(Rufius.desc,xcpt.getIntent());
			intentOut.putExtra(Rufius.unit,intent);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intentOut);

			Rufius.logError(xcpt.getMessage());
		}
		catch(GoogleAuthException xcpt)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Rufius.logError("Authenticator Exception: " + xcpt.getMessage());
			}
		}
		return null;
	}

	//Variables
	private Gmail gmailService;
	private String userEmail;
	private String unitFile;
	private String command;
	private String oldDateString;
	private String serverEmail;
	private Intent incomingIntent;
	private boolean inforOnError;
	private boolean user;
}
