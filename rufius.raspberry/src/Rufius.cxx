#include "Rufius.h"
#include "Rufius_Motion.h"

const std::string Rufius::k_ip_0_255 = std::string("(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
const std::regex Rufius::k_regex_ip = std::regex(Rufius::k_ip_0_255+"\\."+Rufius::k_ip_0_255+"\\."+Rufius::k_ip_0_255+"\\."+Rufius::k_ip_0_255);
const std::regex Rufius::k_regex_rootmail_br = std::regex("root<[a-zA-Z](?:(?:\\.|_)?[a-zA-Z0-9])*[a-zA-Z0-9]@[a-zA-Z][a-zA-Z0-9](?:\\.?[a-zA-Z0-9])*\\.[a-zA-Z]{3,}>");
const std::regex Rufius::k_regex_gmail = std::regex("[a-zA-Z](?:(?:\\.|_)?[a-zA-Z0-9])*[a-zA-Z0-9]@gmail.com");
const std::regex Rufius::k_regex_gmail_br = std::regex("<[a-zA-Z](?:(?:\\.|_)?[a-zA-Z0-9])*[a-zA-Z0-9]@gmail.com>");
const std::regex Rufius::k_regex_smtp = std::regex("mailserver<smtp:\\/\\/[a-zA-Z][a-zA-Z0-9](?:\\.?[a-zA-Z0-9])*\\.[a-zA-Z]{3,}:(?:587|25|465)>");
const std::regex Rufius::k_regex_server_id = std::regex("serverid<[^\\x00-\\x1f\\x7f]+[0-9a-fA-F]{12}>");
const std::regex Rufius::k_regex_motion_path = std::regex("motion<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_snapshots_path = std::regex("snapshots<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_triggers_path = std::regex("triggers<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_key_path = std::regex("key<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_pass_path = std::regex("pass<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_nameserver_path = std::regex("nameservers<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_log_path = std::regex("log<(?:\\/?[^\\x00-\\x1f\\x7f])+>");
const std::regex Rufius::k_regex_user = std::regex("[a-zA-Z0-9\\._][a-zA-Z0-9\\._-]*[$]?<[a-zA-Z](?:(?:\\.|_)?[a-zA-Z0-9])*[a-zA-Z0-9]@gmail.com>");
const std::regex Rufius::k_regex_time = std::regex("gracetime<(?:[1-9][0-9]+)[ \t]*(?:s|sec|m|min)?>");
const std::regex Rufius::k_regex_snapshots_interval = std::regex("snapshots_interval<(?:[1-9][0-9]+)[ \t]*(?:s|sec|m|min)?>");
const std::regex Rufius::k_regex_monitoring_interval = std::regex("monitoring_interval<(?:[1-9][0-9]+)[ \t]*(?:s|sec|m|min)?>");

char* Rufius::email = (char*)0;
char* Rufius::mailserver = (char*)0;
char* Rufius::passwort = (char*)0;
char* Rufius::conf_path = (char*)0;
char* Rufius::password_path = (char*)0;
char* Rufius::rsakey_path = (char*)0;
char* Rufius::nameserver_path = (char*)0;
char* Rufius::log_path = (char*)0;
char* Rufius::server_id = (char*)0;

std::string Rufius::host_ip = "";
std::mutex Rufius::host_ip_lock;

std::unordered_map<int,Rufius_User*> Rufius::users;
std::unordered_set<int> Rufius::users_logined;
std::unordered_set<int> Rufius::users_fencined;

int Rufius::gracetime = 120;

int Rufius::createUsersFromSystem()
{
	int out = 0x0;
	
	struct group* grp;
	struct passwd* psw;
	Rufius_User* usr;
	std::cmatch cm;
	grp = getgrnam("rufians");

	std::string gmail;

	if(grp!=(group*)0)
	{
		if(*grp->gr_mem==(char*)0)
		{
			out = out | 0x02;
		}
		
		while(*grp->gr_mem!=(char*)0)
		{
			gmail.clear();

			psw = getpwnam(*grp->gr_mem);
			if(std::regex_search(psw->pw_gecos,cm,k_regex_gmail_br))
			{
				gmail = std::string(cm[0].str(),1,cm[0].str().size()-2);
			}
			else
			{
				out = out | 0x10000;
			}
			
			usr = new Rufius_User(psw->pw_uid,*grp->gr_mem,gmail.c_str());
						
			users.insert(std::make_pair(usr->getId(),usr));

			grp->gr_mem++;
		}
	}
	else
	{
		out = out | 0x01;
	}
	
	return out;
}

int Rufius::parseConf()
{
	int out = 0x0;
	
	if(!conf_path)
	{
		Rufius_Motion::log(3,"Using Default Configuration",conf_default);
		conf_path = new char[strlen(conf_default)+1];
		strcpy(conf_path,conf_default);
		
		out = out | 0x20000;
	}
	
	struct stat buf;
	if(stat(conf_path,&buf)!=-1)
	{
		Rufius_Motion::log(3,"Using Configuration",conf_path);
		
		std::string line;
		std::ifstream ifstrm(conf_path);
		std::string substring;
		std::string name;
		int indexs;
		int indexe;
		
		struct passwd* psw;
		
		if(ifstrm.is_open())
		{
			Rufius_Motion::log(9,"Parsing Configuration");
			while(getline(ifstrm,line))
			{
				Rufius_Motion::log(9,line);
				
				if((std::regex_match(line,k_regex_motion_path)) && (Rufius_Motion::getConfiguration()==(char*)0))
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-7);
					
					if((substring[0]!='/'))
					{
						substring = std::string(conf_default)+substring;
					}
					
					Rufius_Motion::setConfiguration(substring.c_str());
				}
				else if(std::regex_match(line,k_regex_snapshots_path) && (Rufius_Motion::getSnapshots()==(char*)0))
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-10);
					
					if((substring[0]!='/'))
					{
						substring = std::string(rufius_default)+substring;
					}
					
					Rufius_Motion::setSnapshots(substring.c_str());
				}
				else if(std::regex_match(line,k_regex_triggers_path) && (Rufius_Motion::getTriggers()==(char*)0))
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-9);
					
					if((substring[0]!='/'))
					{
						substring = std::string(rufius_default)+substring;
					}
					
					Rufius_Motion::setTriggers(substring.c_str());
				}
				else if(std::regex_match(line,k_regex_server_id) && server_id==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs,indexe-7);
					
					server_id = new char[substring.size()+1];
					strcpy(server_id,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_rootmail_br) && email==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-5);
					
					email = new char[substring.size()+1];
					strcpy(email,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_smtp) && mailserver==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-11);
					
					mailserver = new char[substring.size()+1];
					strcpy(mailserver,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_pass_path) && password_path==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-5);
					
					if((substring[0]!='/'))
					{
						substring = std::string(conf_default)+substring;
					}
					
					password_path = new char[substring.size()+1];
					strcpy(password_path,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_key_path) && rsakey_path==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-4);
					
					if((substring[0]!='/'))
					{
						substring = std::string(conf_default)+substring;
					}
					
					rsakey_path = new char[substring.size()+1];
					strcpy(rsakey_path,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_nameserver_path) && nameserver_path==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-12);
					
					if((substring[0]!='/'))
					{
						substring = std::string(conf_default)+substring;
					}
					
					nameserver_path = new char[substring.size()+1];
					strcpy(nameserver_path,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_log_path) && log_path==(char*)0)
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					substring = line.substr(indexs+1,indexe-4);
					
					if((substring[0]!='/'))
					{
						substring = std::string(conf_default)+substring;
					}
					
					log_path = new char[substring.size()+1];
					strcpy(log_path,substring.c_str());
				}
				else if(std::regex_match(line,k_regex_time) && (gracetime==0))
				{
					setGraceTime(line);
				}
				else if(std::regex_match(line,k_regex_snapshots_interval) && (Rufius_Motion::snapshotting_interval==0))
				{
					setSnapshotsInterval(line);
				}
				else if(std::regex_match(line,k_regex_monitoring_interval) &&(Rufius_Motion::monitoring_interval==0))
				{
					setMonitoringInterval(line);
				}
				else if(std::regex_match(line,k_regex_user))
				{
					indexs = line.find_first_of('<');
					indexe = line.find_last_of('>');
					
					name = line.substr(0,indexs);
					
					substring = line.substr(indexs+1,indexe-indexs-1);
					
					psw = getpwnam(name.c_str());
					
					if((psw!=(passwd*)0) && ((psw->pw_uid)!=0))
					{
						std::unordered_map<int,Rufius_User*>::const_iterator got = users.find (psw->pw_uid);
						
						if(got!=users.end())
						{
							if(std::regex_match(substring,k_regex_gmail))
							{
								(got->second)->setEmail(substring.c_str());
							}
							
							if(std::regex_search(line,std::regex("[ \t]super(?:[ \t]|$)")))
							{
								(got->second)->setSuper(true);
							}
						}
					}
				}
			}
			
			ifstrm.close();
		}
	}
	else
	{
		out = out | 0x04;
	}
	
	return out;
}

int Rufius::checkConf()
{
	int out = 0x0;
	
	if(server_id==(char*)0)
	{
		out = out | 0x10;
	}
	else
	{
		Rufius_Motion::log(3,"Server ID",server_id);
	}
	
	if(email==(char*)0)
	{
		out = out | 0x08;
		
		Rufius_Motion::log(3,"Server Email not Set");
	}
	else
	{
		Rufius_Motion::log(3,"Server Email Set to",email);
	}
	
	if(mailserver==(char*)0)
	{
		out = out | 0x10;
	}
	else
	{
		Rufius_Motion::log(3,"IMAP Mailserver Set to",mailserver);
	}
	
	if(Rufius_Motion::getConfiguration()==(char*)0)
	{
		Rufius_Motion::setConfiguration(motion_conf_default);
		
		out = out | 0x40000;
		
		Rufius_Motion::log(3,"Using Default Motion Configuration",Rufius_Motion::getConfiguration());
	}
	else
	{
		Rufius_Motion::log(3,"Using Motion Configuration",Rufius_Motion::getConfiguration());
	}
	
	if(Rufius_Motion::getSnapshots()==(char*)0)
	{
		Rufius_Motion::setSnapshots(snapshots_default);
		
		Rufius_Motion::log(3,"Saving Snapshots to Default Location",Rufius_Motion::getSnapshots());
		
		out = out | 0x80000;
	}
	else
	{
		Rufius_Motion::log(3,"Saving Snapshots to",Rufius_Motion::getSnapshots());
	}
	
	if(Rufius_Motion::getTriggers()==(char*)0)
	{
		Rufius_Motion::setTriggers(triggers_default);
		
		Rufius_Motion::log(3,"Saving Triggers to Default Location",Rufius_Motion::getTriggers());
		
		out = out | 0x100000;
	}
	else
	{
		Rufius_Motion::log(3,"Saving Triggers to",Rufius_Motion::getTriggers());
	}
	
	if(Rufius::password_path==(char*)0)
	{
		password_path = new char[strlen(pass_default)+1];
		strcpy(password_path,pass_default);
		
		Rufius_Motion::log(3,"Using Default Password File",password_path);
		
		out = out | 0x20000;
	}
	else
	{
		Rufius_Motion::log(3,"Using Password File",password_path);
	}
	
	if(Rufius::rsakey_path==(char*)0)
	{
		rsakey_path = new char[strlen(key_default)+1];
		strcpy(rsakey_path,key_default);
		
		Rufius_Motion::log(3,"Using Default Key File",rsakey_path);
		
		out = out | 0x400000;
	}
	else
	{
		Rufius_Motion::log(3,"Using Key File",rsakey_path);
	}
	
	if(Rufius::nameserver_path==(char*)0)
	{
		nameserver_path = new char[strlen(nameserver_default)+1];
		strcpy(nameserver_path,nameserver_default);
		
		Rufius_Motion::log(3,"Using Default Nameservers File",nameserver_path);
		
		out = out | 0x800000;
	}
	else
	{
		Rufius_Motion::log(3,"Using Namesrvers File",nameserver_path);
	}
	
	if(Rufius::log_path==(char*)0)
	{
		log_path = new char[strlen(log_default)+1];
		strcpy(log_path,log_default);
		
		Rufius_Motion::log(3,"Saving Log to Default Location",log_path);
		
		out = out | 0x1000000;
	}
	else
	{
		Rufius_Motion::log(3,"Saving Log to",log_path);
	}
	
	if(gracetime!=0)
	{
		Rufius_Motion::log(3,"Grace Time",gracetime);
	}
	
	if(Rufius_Motion::snapshotting_interval!=0)
	{
		Rufius_Motion::log(3,"Snapshotting Interval",Rufius_Motion::snapshotting_interval);
	}
	
	if(Rufius_Motion::monitoring_interval!=0)
	{
		Rufius_Motion::log(3,"Monitoring Interval",Rufius_Motion::monitoring_interval);
	}
	return out;
}

int Rufius::checkFiles()
{
	int out = 0x0;
	
	struct group* grp = getgrnam("rufians");
	
	if(grp==(group*)0)
	{
		out = out | 0x01;
	}
	else
	{
		out = out | 0x20;
		
		for(std::pair<int,Rufius_User*> x : users)
		{
			if((x.second->getEmail()!=(char*)0) && (strlen(x.second->getEmail())>0))
			{
				out = out ^ 0x20;
				break;
			}
		}
		
		unsigned int grid = grp->gr_gid;
		
		setgid(grid);
		umask(0007);
		
		struct stat buf;
		
		int rs = 0;
		
		if((stat(rufius_default,&buf)!=0) || (buf.st_gid!=grid) || ((buf.st_mode & 041750)!=041750))
		{
			std::cout<<std::oct;
			out = out | 0x40;
		}
		
		if((stat(Rufius_Motion::getConfiguration(),&buf)!=0) || (buf.st_gid!=grid) || ((buf.st_mode & 000400)==0))
		{
			out = out | 0x80;
		}
		
		
		if((stat(password_path,&buf)!=0) || (buf.st_gid!=grid) || ((buf.st_mode & 0400)==0))
		{
			out = out | 0x100;
		}
		
		if((stat(rsakey_path,&buf)!=0) || (buf.st_gid!=grid) || ((buf.st_mode & 0400)==0))
		{
			out = out | 0x200;
		}
		
		if((stat(nameserver_path,&buf)!=0) || (buf.st_gid!=grid) || ((buf.st_mode & 0400)==0))
		{
			out = out | 0x400;
		}
		
		if(stat(Rufius_Motion::getSnapshots(),&buf))
		{
			rs = mkdir(Rufius_Motion::getSnapshots(),S_IRWXU|S_IRWXG|S_ISVTX);
			if(rs)
			{
				out = out | 0x1000;
			}
			else
			{
				Rufius_Motion::setSnapshotsList();
			}
		}
		else
		{
			Rufius_Motion::setSnapshotsList();
			
			if(((buf.st_gid)!=grid) || ((buf.st_mode & 01750)!=01750))
			{
				out = out | 0x800;
			}
		}
		
		if(stat(Rufius_Motion::getTriggers(),&buf))
		{
			rs = mkdir(Rufius_Motion::getTriggers(),S_IRWXU|S_IRWXG|S_ISVTX);
			if(rs)
			{
				out = out | 0x2000;
			}
			else
			{
				Rufius_Motion::setTriggersList();
			}
		}
		else
		{
			Rufius_Motion::setTriggersList();
			
			if(((buf.st_gid)!=grid) || ((buf.st_mode & 01750)!=01750))
			{
				out = out | 0x800;
			}
		}
		
		if(stat(log_path,&buf))
		{
			int rs = open(log_path,O_RDWR|O_CREAT,S_IRWXU|S_IRWXG|S_ISVTX);
			
			if(rs<0)
			{
				out = out | 0x4000;
			}
			
			close(rs);
		}
		else if(((buf.st_gid)!=grid) || ((buf.st_mode & 01750)!=01750))
		{
			out = out | 0x800;
		}
		
		if(stat(rufius_tmp,&buf))
		{
			rs = mkdir(rufius_tmp,S_IRWXU|S_IRWXG|S_ISVTX);
			
			if(rs<0)
			{
				out = out | 0x4000;
			}
			
			close(rs);
		}
		else if(((buf.st_gid)!=grid) || ((buf.st_mode & 041750)!=041750))
		{
			out = out | 0x800;
		}
	}
	
	return out;
}

void Rufius::createImagesList(char* dirPath)
{
	struct stat buf;
	char* listFile = new char[strlen(dirPath)+5];
	strcpy(listFile,dirPath);
	strcat(listFile,"/list");
	
	if(stat(listFile,&buf)==0)
	{
		remove(listFile);
	}
	
	int rs = open(listFile,O_RDWR|O_CREAT,S_IRWXU|S_IRWXG|S_ISVTX);
	close(rs);
	
	DIR* dr;
	struct dirent* drst;
	
	dr = opendir(dirPath);
	
	if(dr)
	{
		std::regex imageFile("[0-9-_:]*\\.jpg");
		std::vector<std::string> entries;
		std::string line;
		
		while(drst = readdir(dr))
		{
			line = std::string(drst->d_name);
			
			if(std::regex_match(line,imageFile))
			{
				entries.push_back(line.substr(0,line.size()-4));
			}
		}
		
		std::sort(entries.begin(),entries.end());
		
		std::ofstream output(listFile);
		
		if(output.is_open())
		{
			for(std::string x : entries)
			{
				output<<x;
				output<<"\n";
			}
			
			output.close();
		}
	}
	
	delete listFile;
}

int Rufius::decryptPassword()
{
	int out = 0x8000;
	struct stat buf;
	int ppp = stat(password_path,&buf);
	int rsk = stat(rsakey_path,&buf);
	
	
	if((ppp==0)&&(rsk==0))
	{
		FILE* rsa_pkf = fopen(Rufius::rsakey_path,"r");
		if((rsa_pkf!=(FILE*)0))
		{
			if(SSL_library_init())
			{
				OpenSSL_add_all_algorithms();
				ERR_print_errors_fp(stderr);
			}
			
			const char* pppp = "papardelamauri";
			RSA* rsa_key = PEM_read_RSAPrivateKey(rsa_pkf,NULL,passwordCallback,(void*)pppp);
			ERR_print_errors_fp(stderr);
			
			if(rsa_key)
			{
				int rsa_len = RSA_size(rsa_key);
				
				std::ifstream ifs(Rufius::password_path,std::ifstream::in);
				std::string pss((std::istreambuf_iterator<char>(ifs)),std::istreambuf_iterator<char>());
				
				int fl_len = pss.size();
				unsigned char* pass = new unsigned char[rsa_len];
				RSA_private_decrypt(fl_len,(unsigned char*)pss.c_str(),pass,rsa_key,RSA_SSLV23_PADDING);
				
				int len = strlen((char*)pass);
				Rufius::passwort = new char[len+1];
				strncpy(Rufius::passwort,(char*)pass,len);
				Rufius::passwort[len] = '\0';
				
				delete pass;
				ifs.close();
				
				if(Rufius::passwort!=(char*)0 && (strlen(Rufius::passwort)>0))
				{
					out = out ^ 0x8000;
				}
			}
			
			EVP_cleanup();
		}
		fclose(rsa_pkf);
	}
	
	return out;
}

int Rufius::passwordCallback(char *buf, int size, int rwflag, void *u)
{
	std::cout<<"Please Type Password: ";
	size_t n = 0;
	char* lineptr = (char*)0;
	struct termios old, neues;
	int nread = 0;
	
	/* Turn echoing off and fail if we can't. */
	if (tcgetattr(fileno(stdin), &old) != 0)
	{
		return 0;
	}
	neues = old;
	neues.c_lflag &= ~ECHO;
	if (tcsetattr(fileno(stdin), TCSAFLUSH, &neues) != 0)
	{
		return 0;
	}
	
	/* Read the password. */
	nread = getline(&lineptr, &n, stdin)-1;
	std::cout<<nread<<lineptr;
	
	/* Restore terminal. */
	(void) tcsetattr(fileno(stdin), TCSAFLUSH, &old);
	
	if(nread>1)
	{
		memcpy(buf,lineptr,nread);
	}
	free(lineptr);
	
	std::cout<<"Pass "<<nread<<' '<<buf<<'\n';
	
	return nread;
}

void Rufius::setEmail(const char* cfg)
{
	email = new char[strlen(cfg)+1];
	strcpy(email,cfg);
}

void Rufius::setMailserver(const char* cfg)
{
	mailserver = new char[strlen(cfg)+1];
	strcpy(mailserver,cfg);
}

void Rufius::setConfiguration(const char* cfg)
{
	conf_path = new char[strlen(cfg)+1];
	strcpy(conf_path,cfg);
}

void Rufius::setPassword(const char* cfg)
{
	password_path = new char[strlen(cfg)+1];
	strcpy(password_path,cfg);
}

void Rufius::setRSAKey(const char* cfg)
{
	rsakey_path = new char[strlen(cfg)+1];
	strcpy(rsakey_path,cfg);
}

void Rufius::setNameserver(const char* cfg)
{
	nameserver_path = new char[strlen(cfg)+1];
	strcpy(nameserver_path,cfg);
}

void Rufius::setLog(const char* cfg)
{
	log_path = new char[strlen(cfg)+1];
	strcpy(log_path,cfg);
}

void Rufius::setGraceTime(std::string tm)
{
	std::smatch cm;
	
	if(std::regex_search(tm,cm,std::regex("([1-9][0-9]+)(?:[ \t])*(?:s|sec|m|min)?")))
	{
		gracetime = std::stoi(cm[0].str());
	}
}

void Rufius::setSnapshotsInterval(std::string tm)
{
	std::smatch cm;
	
	if(std::regex_search(tm,cm,std::regex("([1-9][0-9]+)(?:[ \t])*(?:s|sec|m|min)?")))
	{
		Rufius_Motion::snapshotting_interval = std::stoi(cm[0].str());
	}
}

void Rufius::setMonitoringInterval(std::string tm)
{
	std::smatch cm;
	
	if(std::regex_search(tm,cm,std::regex("([1-9][0-9]+)(?:[ \t])*(?:s|sec|m|min)?")))
	{
		Rufius_Motion::monitoring_interval = std::stoi(cm[0].str());
	}
}

void Rufius::destroy()
{
	if(email!=(char*)0)
	{
		delete[] email;
	}
	
	if(mailserver!=(char*)0)
	{
		delete[] mailserver;
	}
	
	if(passwort!=(char*)0)
	{
		delete[] passwort;
	}
	
	if(conf_path!=(char*)0)
	{
		delete[] conf_path;
	}
	
	if(password_path!=(char*)0)
	{
		delete[] password_path;
	}
	
	if(rsakey_path!=(char*)0)
	{
		delete[] rsakey_path;
	}
	
	if(nameserver_path!=(char*)0)
	{
		delete[] nameserver_path;
	}
	
	if(log_path!=(char*)0)
	{
		delete[] log_path;
	}
	
	for(std::pair<int,Rufius_User*> x : users)
	{
		delete x.second;
	}
}

void Rufius::logErrors(int ercd)
{
	if(ercd&0x0001)
	{
		Rufius_Motion::log(0,"Group rufians does not exist");
	}
	
	if(ercd&0x0002)
	{
		Rufius_Motion::log(0,"Group rufians has 0 Members");
	}
	
	if(ercd&0x0004)
	{
		Rufius_Motion::log(0,"Configuration File Error");
	}
	
	if(ercd&0x0008)
	{
		Rufius_Motion::log(0,"Server Email Address Error");
	}
	
	if(ercd&0x0010)
	{
		Rufius_Motion::log(0,"Mailserver Error");
	}
	
	if(ercd&0x0020)
	{
		Rufius_Motion::log(0,"User Email Address Error");
	}
	
	if(ercd&0x0040)
	{
		Rufius_Motion::log(0,"Install Directory Error");
	}
	
	if(ercd&0x0080)
	{
		Rufius_Motion::log(0,"Motion Configuration File Error");
	}
	
	if(ercd&0x0100)
	{
		Rufius_Motion::log(0,"Password File Error");
	}
	
	if(ercd&0x0200)
	{
		Rufius_Motion::log(0,"Key File Error");
	}
	
	if(ercd&0x0400)
	{
		Rufius_Motion::log(0,"Nameservers File Error");
	}
	
	if(ercd&0x0800)
	{
		Rufius_Motion::log(0,"Permissions not set properly");
	}
	
	if(ercd&0x1000)
	{
		Rufius_Motion::log(0,"Cannot create snapshots directory");
	}
	
	if(ercd&0x2000)
	{
		Rufius_Motion::log(0,"Cannot create triggers directory");
	}
	
	if(ercd&0x4000)
	{
		Rufius_Motion::log(0,"Cannot create Log File");
	}
	
	if(ercd&0x8000)
	{
		Rufius_Motion::log(0,"Password Error");
	}
}
