package lab.drys.rufius.utilities;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;

import lab.drys.rufius.Rufius;

public class Commander
{
	public Commander()
	{
		this.status = 0x7f0000;
		this.jsch = null;
		this.session = null;
		this.host_key = null;
	}

	public boolean initiate(String knownHosts)
	{
		boolean out = false;
		int trials = 10;
		jsch = null;
		while(trials>0)
		{
			try
			{
				jsch = new JSch();
				jsch.setKnownHosts(knownHosts);

				status = 0x3f0000;
				out = true;
				break;
			}
			catch(JSchException xcpt0)
			{
				if(!Rufius.RELEASE_FLAG)
				{
					Rufius.logError("JSch Channel Exception: " + xcpt0.getMessage());
				}

				try
				{
					Thread.sleep(30000);
				}
				catch(InterruptedException xcpt1)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("JSch Thread Sleep Exception: " + xcpt1.getMessage());
					}
				}
			}

			trials--;
		}

		return out;
	}

	public boolean initiate(String knownHosts, String privateKey)
	{
		boolean out = false;
		int trials = 10;
		jsch = null;
		while(trials>0)
		{
			try
			{
				jsch = new JSch();
				jsch.setKnownHosts(knownHosts);
				jsch.addIdentity(privateKey);

				status = 0x3f0000;
				out = true;
				break;
			}
			catch(JSchException xcpt0)
			{
				if(!Rufius.RELEASE_FLAG)
				{
					Rufius.logError("JSch Channel Exception: " + xcpt0.getMessage());
				}

				try
				{
					Thread.sleep(30000);
				}
				catch(InterruptedException xcpt1)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("JSch Thread Sleep Exception: " + xcpt1.getMessage());
					}
				}
			}

			trials--;
		}

		return out;
	}

	public boolean prepare(boolean flag, String username, String ip, int port)
	{
		boolean out = false;

		if(jsch != null)
		{
			int trials = 10;

			while(trials>0)
			{
				try
				{
					session = jsch.getSession(username, ip, port);
					if(flag)
					{
						java.util.Properties config = new java.util.Properties();
						config.put("StrictHostKeyChecking", "no");
						session.setConfig(config);
					}

					status = 0x1f0000;
					out = true;
					break;
				}
				catch(JSchException xcpt0)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("JSch Session Exception: " + xcpt0.getMessage());
					}

					try
					{
						Thread.sleep(30000);
					}
					catch(InterruptedException xcpt1)
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Rufius.logError("JSch Thread Sleep Exception: " + xcpt1.getMessage());
						}
					}
				}

				trials--;
			}
		}

		return out;
	}

	public boolean prepare(boolean flag_NoKeyCheck, String username, String ip, int port, String password)
	{
		boolean out = false;

		if(jsch != null)
		{
			int trials = 10;

			while(trials>0)
			{
				try
				{
					session = jsch.getSession(username, ip, port);
					session.setPassword(password);
					if(flag_NoKeyCheck)
					{
						java.util.Properties config = new java.util.Properties();
						config.put("StrictHostKeyChecking", "no");
						session.setConfig(config);
					}

					status = 0x1f0000;
					out = true;
					break;
				}
				catch(JSchException xcpt0)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("JSch Session Exception: " + xcpt0.getMessage());
					}

					try
					{
						Thread.sleep(30000);
					}
					catch(InterruptedException xcpt1)
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Rufius.logError("JSch Thread Sleep Exception: " + xcpt1.getMessage());
						}
					}
				}

				trials--;
			}
		}

		return out;
	}

	public boolean connect()
	{
		boolean out = false;

		if(session != null)
		{
			try
			{
				session.connect(30000);

				status = 0x30000;

				out = true;
			}
			catch(JSchException xcpt0)
			{
				String msg = xcpt0.getMessage();

				if(msg.contains("Auth Fail") || msg.contains("timeout") || msg.contains("ECONNREFUSED"))
				{
					status = 0xf0000;
				}
				else if(msg.contains("UnknownHostKey"))
				{
					status = 0x70000;
				}

				if(!Rufius.RELEASE_FLAG)
				{
					Rufius.logError("JSch Connection Exception: " + xcpt0.getMessage());
				}

				host_key = session.getHostKey();

				if(host_key != null)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logDebug("Server Fingerprint: " + host_key.getFingerPrint(jsch));
						Rufius.logDebug("Host: " + host_key.getHost());
						Rufius.logDebug("Key: " + host_key.getKey());
						Rufius.logDebug("Marker: " + host_key.getMarker());
						Rufius.logDebug("Comment: " + host_key.getComment());
					}
				}

				session.disconnect();
			}
		}

		return out;
	}

	public void download(String source, String destination, boolean disconnect)
	{
		if(session.isConnected())
		{
			Channel channel;
			ChannelSftp channelSftp = null;
			try
			{
				status = 0x10000;
				channel = session.openChannel("sftp");
				channel.connect();
				channelSftp = (ChannelSftp)channel;
				Rufius.logDebug("Download Begin");

				try
				{
					Rufius.logDebug("Download Start");
					channelSftp.get(source,destination);
					status = 0x80;
					Rufius.logDebug("Download Success");
				}
				catch(SftpException xcpt0)
				{
					Rufius.logDebug("Download Failure");
					Rufius.logDebug(xcpt0.getMessage());
				}

			}
			catch(JSchException xcpt)
			{
				Rufius.logDebug(xcpt.getMessage());
			}

			if(channelSftp!=null)
			{
				channelSftp.disconnect();
			}

			if(disconnect)
			{
				session.disconnect();
			}
		}
	}

	public void execute(String comm, boolean disconnect)
	{
		if(session.isConnected())
		{
			int trials = 10;

			while(trials>0)
			{
				try
				{
					status = 0x10000;
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logInfo("Executing... " + comm);
					}

					ChannelExec channel = (ChannelExec)session.openChannel("exec");

					channel.setCommand(comm);

					channel.setInputStream(null);
					channel.setErrStream(System.err);

					String reception="";
					try
					{
						InputStream in = channel.getInputStream();
						channel.connect();
						byte[] tmp = new byte[1024];

						while((!channel.isClosed())||in.available()>0)
						{
							int i = in.read(tmp, 0, 1024);
							while(i>0)
							{
								reception+= new String(tmp,0,i);
								i = in.read(tmp, 0, 1024);
							}

							try
							{
								Thread.sleep(1000);
							}
							catch(InterruptedException xcpt2)
							{

							}
						}
					}
					catch(IOException xcpt1)
					{

					}


					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logInfo("Received: " + reception);
					}

					reception = reception.replaceAll("[^0-9]", "");
					if(!reception.isEmpty())
					{
						try
						{
							status = (Long.valueOf(reception)).intValue();
						}
						catch(NumberFormatException e)
						{
							if(!Rufius.RELEASE_FLAG)
							{
								Rufius.logError("Number Format Exception: " + e.getMessage());
								Rufius.logInfo(reception);
							}
						}
					}

					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logInfo("Status Received: " + reception);
					}

					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logInfo("Channel Exit: " + channel.getExitStatus());
					}

					channel.disconnect();
					break;
				}
				catch(JSchException xcpt)
				{
					if(!Rufius.RELEASE_FLAG)
					{
						Rufius.logError("JSch Command Exception: " + xcpt.getMessage());
					}

					try
					{
						Thread.sleep(1000);
					}
					catch(InterruptedException xcpt0)
					{
						if(!Rufius.RELEASE_FLAG)
						{
							Rufius.logError("JSch Thread Sleep Exception: " + xcpt0.getMessage());
						}
					}
				}

				trials--;
			}

			if(disconnect)
			{
				session.disconnect();
			}
		}
	}

	public int getStatus()
	{
		return status;
	}

	public String getFingerPrint()
	{
		String out = null;
		if((status<0xf0000) && (host_key!=null))
		{
			out = host_key.getFingerPrint(jsch);
		}
		return out;
	}

	public String getKey()
	{
		String out = null;
		if((status<0xf0000) && (host_key!=null))
		{
			out = host_key.getKey();
		}
		return out;
	}

	public String getKeyType()
	{
		String out = null;
		if((status<0xf0000) && (host_key!=null))
		{
			out = host_key.getType();
		}
		return out;
	}

	public String getHostInfo()
	{
		String out = null;
		if((status<0xf0000) && (host_key!=null))
		{
			out = host_key.getHost()+" "+host_key.getType()+" "+host_key.getKey();
		}
		return out;
	}

	//Variables
	private JSch jsch;
	private Session session;
	private int status;

	private HostKey host_key;
}
