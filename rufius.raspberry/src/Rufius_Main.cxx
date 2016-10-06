#include "Rufius_Main.h"
#include <Rufius_Informer.h>

unsigned int Rufius_Main::config = 0x0;
int Rufius_Main::message = Rufius::EMPTY;
int Rufius_Main::messageExtra = 0;

void Rufius_Main::parseArguments(int argc, char* argv[])
{
	std::map<char, unsigned int> shortArguments = {
	{'?',0x0002},
	{'d',0x0004},
	{'m',0x0008},
	{'v',0x0010},
	{'e',0x0020},
	{'g',0x0040},
	{'s',0x0080},
	{'t',0x0100},
	{'l',0x0200},
	{'k',0x0400},
	{'p',0x0800},
	{'c',0x1000},
	{'o',0x2000},
	{'n',0x4000},
	{'i',0x8000}
	};

	std::map<std::string, unsigned int> longArguments = {
	{"--help",0x0002},
	{"--daemon",0x0004},
	{"--message",0x0008},
	{"--verbose",0x0010},
	{"--email",0x0020},
	{"--grace-time",0x0040},
	{"--snapshots-path",0x0080},
	{"--triggers-path",0x0100},
	{"--log-path",0x0200},
	{"--key-path",0x0400},
	{"--password-path",0x0800},
	{"--conf-path",0x1000},
	{"--motion-conf-path",0x2000},
	{"--monitoring-interval",0x4000},
	{"--snapshot-interval",0x8000}
	};

	if(argc>1)
	{
		boolean ok = true;
		unsigned int cf = 0;
		unsigned int rv = 0;

		for(int i=1;i<argc && ok ;i++)
		{
			size_t argLength = strlen(argv[i]);
			ok = false;

			if((argLength==2) && (argv[i][0]=='-') && (rv!=2))
			{
				if(shortArguments.find(argv[i][1])!=shortArguments.end())
				{
					cf = shortArguments.at(argv[i][1]);

					if((config&cf)==0)
					{
						config = config | cf;
						rv = requiresVariable(cf);
						ok = true;
					}
				}
			}
			else if((argLength>2) && (argv[i][0]=='-') && (argv[i][1]!='-') && (rv!=2))
			{
				rv = 0;
				unsigned int rcf = 0;
				unsigned int nrv = 0;
				for(int j=1; j<argLength; j++)
				{
					ok = false;
					if(shortArguments.find(argv[i][j])!=shortArguments.end())
					{
						rcf = shortArguments.at(argv[i][j]);

						if((config&rcf)==0)
						{
							config = config | rcf;
							nrv = requiresVariable(rcf);

							if((nrv==2))
							{
								if(rv==2)
								{
									break;
								}
								
								rv = nrv;
								cf = rcf;
							}
							else if((nrv==1) &&(rv!=2))
							{
								rv = nrv;
								cf = rcf;
							}
							ok = true;
						}
						else
						{
							break;
						}
					}
					else
					{
						break;
					}
				}
			}
			else if((argLength>3) && (argv[i][0]=='-') && (argv[i][1]=='-') && (rv!=2))
			{
				std::string argmnt(argv[i]);
				if((longArguments.find(argmnt))!=longArguments.end())
				{
					cf = longArguments.at(argmnt);

					if((config&cf)==0)
					{
						config = config | cf;
						rv = requiresVariable(cf);

						ok = true;
					}
				}
			}
			else if((rv==2) || (rv==1))
			{
				ok=true;
				rv=0;
				if(cf==0x0010)
				{
					checkVerbosity(argv[i]);
				}
				else if(cf==0x0008)
				{
					rv = checkMessage(argv[i]);
				}
				else if(cf&0x3f80)
				{
					checkPath(argv[i],cf);
				}
				else if(cf&0x4000)
				{
					int nm = parseNumber(argv[i]);
					if(nm>0)
					{
						Rufius_Motion::setMonitoringInterval(nm);
					}
				}
				else if(cf&0x8000)
				{
					int nm = parseNumber(argv[i]);
					if(nm>0)
					{
						Rufius_Motion::setSnapshottingInterval(nm);
					}
				}
			}
			else if(rv==3)
			{
				message = message | Rufius::MSGEXTRA;
				messageExtra = parseNumber(argv[i]);
				rv = 0;
			}
		}
		
		if(ok && ((config==0x0002) || ((config&0x000e)==0x0004) || ((config&0xffee)==0x0008)))
		{
			if((config & 0x010))
			{
				if(Rufius_Motion::verbosityLevel==-2)
				{
					std::cout<<"Invalid Verbosity Variable.\n\n";
					ok = false;
				}
				else if(Rufius_Motion::verbosityLevel==-1)
				{
					Rufius_Motion::verbosityLevel = 0;
				}
			}

			if((config & 0x08) && message==Rufius::EMPTY)
			{
				std::cout<<"Invalid Message.\n\n";
				ok = false;
			}

			if(ok)
			{
				config = config | 0x01;
			}
		}
	}
}

unsigned int Rufius_Main::requiresVariable(unsigned int type)
{
	unsigned int out = 0;

	if(type & 0xffe8)
	{
		out = 2;
	}
	else if(type & 0x0010)
	{
		out = 1;
	}

	return out;
}

void Rufius_Main::checkEmail(char* msg)
{
	std::string pass(msg);
	
	if(std::regex_match(msg,Rufius::k_regex_rootmail_br))
	{
		Rufius::setEmail(msg);
	}
}

int Rufius_Main::checkMessage(char* msg)
{
	int out = 0;
	
	if(!strcmp(msg,"status"))
	{
		message = Rufius::STATUS;
	}
	else if(!strcmp(msg,"infon"))
	{
		message = Rufius::INFON;
	}
	else if(!strcmp(msg,"infoff"))
	{
		message = Rufius::INFOFF;
	}
	else if(!strcmp(msg,"inwifi"))
	{
		message = Rufius::INWIFI;
	}
	else if(!strcmp(msg,"outwifi"))
	{
		message = Rufius::OUTWIFI;
	}
	else if(!strcmp(msg,"infence"))
	{
		message = Rufius::INFENCE;
	}
	else if(!strcmp(msg,"outfence"))
	{
		message = Rufius::OUTFENCE;
	}
	else if(!strcmp(msg,"deton"))
	{
		message = Rufius::DETON;
		
		out =1 ;
	}
	else if(!strcmp(msg,"detoff"))
	{
		message = Rufius::DETOFF;
	}
	else if(!strcmp(msg,"snap"))
	{
		message = Rufius::SNAP;
	}
	else if(!strcmp(msg,"snapon"))
	{
		message = Rufius::SNAPON;
		
		out = 1;
	}
	else if(!strcmp(msg,"snapoff"))
	{
		message = Rufius::SNAPOFF;
			}
	else if(!strcmp(msg,"camon"))
	{
		message = Rufius::CAMON;
		
		out = 1;
	}
	else if(!strcmp(msg,"camoff"))
	{
		message = Rufius::CAMOFF;
	}
	else if(!strcmp(msg,"toggle"))
	{
		message = Rufius::TOGGLE;
	}
	else
	{
		message = Rufius::EMPTY;
	}
	
	return out;
}

void Rufius_Main::checkVerbosity(char* msg)
{
	if(strlen(msg)==1 && (msg[0]>0x2f) && (msg[0]<0x3a))
	{
		Rufius_Motion::verbosityLevel = msg[0]-0x30;
	}
	else
	{
		Rufius_Motion::verbosityLevel = -2;
	}
}

void Rufius_Main::checkPath(char* msg, int type)
{
	switch(type)
	{
		case 0x0080 :
			Rufius_Motion::setSnapshots(msg);
		break;
		case 0x0100 :
			Rufius_Motion::setTriggers(msg);
		break;
		case 0x0200 :
			Rufius::setLog(msg);
		break;
		case 0x0400 :
			Rufius::setRSAKey(msg);
		break;
		case  0x0800 :
			Rufius::setPassword(msg);
		break;
		case 0x1000 :
			Rufius::setConfiguration(msg);
		break;
		case 0x2000 :
			Rufius_Motion::setConfiguration(msg);
		break;
	}
}

int Rufius_Main::parseNumber(char* msg)
{
	int out = -1;
	
	std::regex nmb("[0-9]*");
	
	if(std::regex_match(msg,nmb))
	{
		out = std::stoi(msg);
	}
	
	return out;
}

void Rufius_Main::printUsage()
{
	std::cout<<"Usage : rufius (-?) | (-d [-vegstlkpconi]) | (-m [-v])"
	<<'\n'<<"(-? --help)\t\t\t\t\t\t\t: Print Usage.\n"
	<<'\n'<<"(-d --daemon)\t\t\t\t\t\t\t: Start the Server.\n"
	<<'\n'<<"(-m --message)\t\t\t{Valid Message}\t\t\t: Send a message.\n"
	<<'\n'<<"(-v --verbose)\t\t\t[0-9]\t\t\t\t: Set Log Output Verbosity Level.\n"
	<<'\n'<<"(-e --email)\t\t\t{wxyzwxyz@xyzxyz.xyz}\t\t: Set the Server's Email.\n"
	<<'\n'<<"(-g --grace-time)\t\t{seconds}\t\t\t: Set Allowance Time for Users to declare their Presence.\n"
	<<'\n'<<"(-s --snapshots-path)\t\t{Absolute / Relative Path}\t: Set the Directory to save Snapshot Images to.\n"
	<<'\n'<<"(-t --triggers-path)\t\t{Absolute / Relative Path}\t: Set the Directory to save Motion Images to.\n"
	<<'\n'<<"(-l --log-path)\t\t\t\{Absolute / Relative Path}\t: Set the File to write Logs.\n"
	<<'\n'<<"(-k --key-path)\t\t\t{Absolute / Relative Path}\t: Set the Key File.\n"
	<<'\n'<<"(-p --password-path)\t\t{Absolute / Relative Path}\t: Set the Encrypted Password File.\n"
	<<'\n'<<"(-c --conf-path)\t\t{Absolute / Relative Path}\t: Set the Main Configuration File.\n"
	<<'\n'<<"(-o --motion-conf-path)\t\t{Absolute / Relative Path}\t: Set the Motion Configuration File.\n"
	<<'\n'<<"(-n --monitoring-interval)\t{seconds}\t\t\t: Set the Interval for snapshotting when in Detection.\n"
	<<'\n'<<"(-i --snapshot-interval)\t{seconds}\t\t\t: Set the Interval for snapshotting when not in Detection.\n"
	<<"\n";
}

void Rufius_Main::init()
{
	int uid = getuid();

	if((config & 0x01) & ((config ^ 0x02)>>1))
	{
		if((config & 0x008))
		{
			sendMessage();
		}
		else if(uid==0)
		{
			if(config & 0x004)
			{
				startServer();
			}
		}
		else
		{
			Rufius_Motion::log(0,"Only root can start Daemon!");
		}
	}
	else
	{
		printUsage();
	}
}

void Rufius_Main::destroy()
{
	Rufius_Informer::destroy();
	Rufius_Motion::destroy();
	Rufius::destroy();
}


void Rufius_Main::startServer()
{
	if(!isRunning())
	{
		struct stat buf;
		if(stat(rufius_socket,&buf)==0)
		{
			remove(rufius_socket);
		}
		
		int rs = Rufius::createUsersFromSystem();
		if((rs & 0x0000ffff)==0)
		{
			rs = Rufius::parseConf();
			if((rs & 0x0000ffff)==0)
			{
				rs = Rufius::checkConf();
				if((rs & 0x0000ffff)==0)
				{
					Rufius_Motion::on_detection_function = Rufius_Informer::on_detection;
					Rufius_Motion::on_area_detection_function = Rufius_Informer::on_area_detection;
					Rufius_Motion::on_event_start_function = Rufius_Informer::on_event_start;
					Rufius_Motion::on_event_end_function = Rufius_Informer::on_event_end;
					Rufius_Motion::on_camera_lost_function = Rufius_Informer::on_camera_lost;
					Rufius_Motion::on_picture_save_function = Rufius_Informer::on_picture_save;
					
					rs = Rufius::checkFiles();
					if((rs&0x0000ffff)==0)
					{
						rs = Rufius::decryptPassword();
						Rufius::createImagesList(Rufius_Motion::getSnapshots());
						Rufius::createImagesList(Rufius_Motion::getTriggers());
						if((rs&0x0000ffff)==0)
						{
							Rufius_Informer::init();
							Rufius_Daemon::instance();
						}
					}
				}
			}
		}
		
		if(rs)
		{
			Rufius::logErrors(rs);
		}
	}
	else
	{
		exit(EXIT_FAILURE);
	}
	
	Rufius_Main::destroy();
}

bool Rufius_Main::isRunning()
{
	bool out = false;
	DIR* dr;
	struct dirent* drst;
		
	dr = opendir("/proc/");
	
	if(dr)
	{
		std::string procDir("/proc/");
		std::string name;
		std::string cmdline("/cmdline");
		std::regex nmb("[0-9]*");
		std::regex rufiusCmd("(rufius)");
		std::regex daemon("(-d)|(--daemon)");
		struct stat buf;
		std::string line;
		std::ifstream ifstream;
		
		while(drst = readdir(dr))
		{
			name = std::string(drst->d_name);
			if(std::regex_match(name,nmb))
			{
				if(stat((procDir+name+cmdline).c_str(),&buf)==0)
				{
					ifstream = std::ifstream(procDir+name+cmdline);
					if(ifstream.is_open())
					{
						if(getline(ifstream,line))
						{
							if((line.size()>=6) && (line.compare(0,6,"rufius")==0) && (std::regex_search(line,daemon)))
							{
								int pid = getpid();
								
								
								if(name.compare(std::to_string(pid))!=0)
								{
									out = true;
									break;
								}
							}
						}
						
						ifstream.close();
					}
				}
			}
		}
	}
	
	return out;
}

void Rufius_Main::sendMessage()
{
	uid_t ui = getuid();
	
	int result = 0;

	int client_socket_fd = socket(PF_LOCAL,SOCK_STREAM,0);
	struct sockaddr_un name;
	name.sun_family = AF_LOCAL;
	strcpy(name.sun_path,rufius_socket);
	int cnt = connect(client_socket_fd,(const sockaddr*)&name,SUN_LEN(&name));
	if(cnt<0)
	{

	}
	else
	{
		write(client_socket_fd,&ui,sizeof(int));
		write(client_socket_fd,&message,sizeof(int));
		
		if(message&Rufius::MSGEXTRA)
		{
			write(client_socket_fd,&messageExtra,sizeof(int));
		}
		
		read(client_socket_fd,&result,sizeof(int));
	}
	close(client_socket_fd);

	std::cout<<result<<"\n";
}
