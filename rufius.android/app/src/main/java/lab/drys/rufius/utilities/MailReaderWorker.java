/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab.drys.rufius.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lab.drys.rufius.Rufius;

public final class MailReaderWorker
{
	//Variables
	private Context context;
	private int trials;
	private String email;
	private String emailpass;
	private String rufiusemail;
	private Address rufAddress;

	public MailReaderWorker(Context cnt)
	{
		context = cnt;
		trials = 10;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		email = sharedPreferences.getString("Rufemail", "");
		emailpass = sharedPreferences.getString("Rufemailpass", "");
		rufiusemail = sharedPreferences.getString("HostEmail", "");

		rufAddress = new InternetAddress();
	}

	private static String getText(BodyPart p)
			throws
			MessagingException, IOException
	{
		String s = null;
		if(p.isMimeType("text/*"))
		{
			s = (String) p.getContent();
			//p.isMimeType("text/html");

		}

		return s;
	}

	public void init()
	{
		try
		{
			rufAddress = new InternetAddress(rufiusemail);
		}
		catch(AddressException e)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Log.i("Rufius", e.toString());
			}

			if(trials > 0)
			{
				try
				{
					Thread.sleep(5000);
				}
				catch(InterruptedException ie)
				{
				}
				trials--;
				this.init();
			}
		}
	}

	public void readIP()
	{
		try
		{
			Properties props = new Properties();
			props.setProperty("mail.store.property", "imaps");
			//props.put("mail.imap.ssl.enable", "true");
			//props.put("mail.imap.sasl.enable", "true");
			//props.put("mail.imap.sasl.mechanisms", "XOAUTH2");
			//props.put("mail.imap.auth.login.disable", "true");
			//props.put("mail.imap.auth.plain.disable", "true");

			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imaps");
			store.connect("imap.gmail.com", email, emailpass);

			if(!Rufius.RELEASE_FLAG)
			{
				Log.i("Rufius", store.toString());
			}

			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_WRITE);
			Message messages[] = inbox.getMessages();
			Address address[];
			Message last = null;

			for(Message msg : messages)
			{
				address = msg.getFrom();

				for(Address addr : address)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Log.i("Rufius", ((InternetAddress) addr).getAddress());
					}
					if(addr.equals(rufAddress))
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Log.i("Rufius", "We have rufius");
						}
						if(msg.getSubject().equals("IPChange") || msg.getSubject().equals("IP_Change") || msg.getSubject().equals("IPChange."))
						{
							if(!Rufius.RELEASE_FLAG)
							{
								Log.i("Rufius", "We have a new IP");
							}
							if(last != null)
							{
								if(msg.getSentDate().after(last.getSentDate()))
								{
									if(!Rufius.RELEASE_FLAG)
									{
										Log.i("Rufius", "We have a brand new IP");
									}
									last.setFlag(Flags.Flag.DELETED, true);
									last = msg;
								}
							}
							else
							{
								last = msg;
							}
						}
						else
						{
							msg.setFlag(Flags.Flag.DELETED, true);
						}
					}
					else
					{
						msg.setFlag(Flags.Flag.DELETED, true);
					}
				}
			}

			if(last != null)
			{
				if(!Rufius.RELEASE_FLAG)
				{
					Log.i("Rufius", last.getContentType());
				}

				try
				{
					Object ob = last.getContent();

					if(ob instanceof String)
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Log.i("Rufius", "String");
						}
						String str = ((String) ob).trim();
						if(!Rufius.RELEASE_FLAG)
						{
							Log.i("Rufius", str);
						}
						if(str.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$"))
						{
							if(!Rufius.RELEASE_FLAG)
							{
								Log.i("Rufius", "Wir sind perfekt");
							}
							if(!Rufius.RELEASE_FLAG)
							{
								Log.i("Rufius", PreferenceManager.getDefaultSharedPreferences(context).getString("HostIP", "192.168.1.4"));
							}
							PreferenceManager.getDefaultSharedPreferences(context).edit().putString("HostIP", str).commit();
							if(!Rufius.RELEASE_FLAG)
							{
								Log.i("Rufius", PreferenceManager.getDefaultSharedPreferences(context).getString("HostIP", "192.168.1.4"));
							}
						}
					}
					else if(ob instanceof Multipart)
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Log.i("Rufius", "Multipart");
						}
						if(!Rufius.RELEASE_FLAG)
						{
							Log.i("Rufius", ((Multipart) ob).getContentType());
						}

						for(int i = 0; i < ((Multipart) ob).getCount(); i++)
						{
							if(((Multipart) ob).getBodyPart(i).isMimeType("text/html"))
							{
								if(!Rufius.RELEASE_FLAG)
								{
									Log.i("Rufius", "Grand Succes");
								}
								String s = MailReaderWorker.getText(((Multipart) ob).getBodyPart(i));
								if(!Rufius.RELEASE_FLAG)
								{
									Log.i("Rufius", s);
								}
							}
							else if(((Multipart) ob).getBodyPart(i).isMimeType("text/plain"))
							{
								if(!Rufius.RELEASE_FLAG)
								{
									Log.i("Rufius", "Another Succes");
								}
								String s = MailReaderWorker.getText(((Multipart) ob).getBodyPart(i));
								if(!Rufius.RELEASE_FLAG)
								{
									Log.i("Rufius", s);
								}
							}
							else
							{
								if(!Rufius.RELEASE_FLAG)
								{
									Log.i("Rufius", "No Succes");
								}
							}
						}
					}
				}
				catch(IOException e)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Log.i("Rufius", e.toString());
					}
				}
			}
			inbox.close(true);
		}
		catch(MessagingException e)
		{
			if(!Rufius.RELEASE_FLAG)
			{
				Log.i("Rufius", "Network " + e.toString());
			}

			if(trials > 0)
			{
				try
				{
					Thread.sleep(5000);
				}
				catch(InterruptedException ie)
				{
				}
				trials--;
				this.readIP();
			}
		}
	}
}
