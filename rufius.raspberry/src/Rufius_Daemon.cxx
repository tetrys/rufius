#include "Rufius_Daemon.h"
#include "Rufius_Informer.h"

pid_t Rufius_Daemon::pid = -1;
pid_t Rufius_Daemon::sid = -1;
int Rufius_Daemon::socket_fd = -1;

std::condition_variable Rufius_Daemon::main_cv;
std::condition_variable Rufius_Daemon::ip_cv;
std::condition_variable Rufius_Daemon::sn_cv;
std::condition_variable Rufius_Daemon::gt_cv;
std::condition_variable Rufius_Daemon::if_cv;

std::thread Rufius_Daemon::getIP_thread;
std::thread Rufius_Daemon::server_thread;
std::thread Rufius_Daemon::snapshotting_thread;
std::thread Rufius_Daemon::gracetime_thread;
std::thread Rufius_Daemon::informer_thread;

void Rufius_Daemon::instance()
{
	Rufius_Motion::log(9,"Creating Daemon New Instatnce");

	init();
	start();
}

void Rufius_Daemon::init()
{
	Rufius_Motion::log(9,"Daemon Initiation");
	/* Fork off the parent process */
	pid = fork();
	if (pid < 0)
	{
		Rufius_Motion::log(3,"Daemon Initiation Error, Adorting");
		exit(EXIT_FAILURE);
	}

	/* If we got a good PID, then
	we can exit the parent process. */
	if (pid > 0)
	{
		Rufius_Motion::log(9,"Daemon Initiation Successful");
		exit(EXIT_SUCCESS);
	}

	/* Change the file mode mask */
	umask(0);

	/* Open any logs here */

	/* Create a new SID for the child process */
	sid = setsid();
	if (sid < 0)
	{
		Rufius_Motion::log(9,"Daemon SID ",sid);
		/* Log the failure */
		exit(EXIT_FAILURE);
	}

	/* Change the current working directory */
	if ((chdir("/")) < 0)
	{
		Rufius_Motion::log(9,"Seting working dir / Failure");
		/* Log the failure */
		exit(EXIT_FAILURE);
	}
	Rufius_Motion::log(9,"Working Dir Changed to /");
	
	/* Close out the standard file descriptors */
	if(Rufius_Motion::verbosityLevel<0)
	{
		close(STDIN_FILENO);
		close(STDOUT_FILENO);
		close(STDERR_FILENO);
	}
}

void Rufius_Daemon::start()
{
	Rufius_Motion::log(9,"Daemon Starting");
	Rufius_Motion::start();

	server_thread = std::thread(server_task);
	getIP_thread = std::thread(getIP_task);
	gracetime_thread = std::thread(gracetime_task);
	informer_thread = std::thread(Rufius_Informer::inform_init);
	
	signal(SIGTERM, Rufius_Daemon::signalhandler);

	if(gracetime_thread.joinable())
	{
		gracetime_thread.join();
	}
	
	//Main Thread
	while (Rufius_Motion::isRunning())
	{
		if(Rufius_Motion::isRunning())
		{
			std::mutex mtx;
			std::unique_lock<std::mutex> lck(mtx);
			main_cv.wait(lck);
		}

		Rufius_Motion::log(0,"Main Thread Finished.\n");
	}

	server_thread.join();
	getIP_thread.join();
	if(snapshotting_thread.joinable())
	{
		snapshotting_thread.join();
	}
	if(informer_thread.joinable())
	{
		informer_thread.join();
	}
}

void Rufius_Daemon::stop()
{
	Rufius_Motion::log(3,"Daemon Stopping");
	Rufius_Motion::stop();
	
	ip_cv.notify_all();
	main_cv.notify_all();
	gt_cv.notify_all();
	sn_cv.notify_all();
	stopMonitoring();
	
	shutdown(socket_fd,SHUT_RDWR);
}

void Rufius_Daemon::updateStatus()
{
	if((Rufius::users_logined.size()==0)&&(!(Rufius_Motion::isDetecting())))
	{
		Rufius_Motion::startDetection();
	}
	else if((Rufius::users_logined.size()>0)&&(Rufius_Motion::isDetecting()))
	{
		Rufius_Motion::stopDetection();
	}
}

void Rufius_Daemon::server_task()
{
	Rufius_Motion::log(7,"Server Thread (rufius-thread) Initiation");
	prctl(PR_SET_NAME,"rufius-server",0,0,0);
	
	Rufius_Daemon::socket_fd = socket(PF_LOCAL,SOCK_STREAM,0);
	if(Rufius_Daemon::socket_fd<0)
	{
		Rufius_Motion::log(3,"Server Socket Creation Error");
	}

	struct sockaddr_un name;
	name.sun_family = AF_LOCAL;
	strcpy(name.sun_path,rufius_socket);
	int bnd = bind(socket_fd,(struct sockaddr*)&name,SUN_LEN(&name));

	if(bnd<0)
	{
		Rufius_Motion::log(3,"Server Bind Error");
		remove(rufius_socket);
		Rufius_Daemon::stop();
		return;
	}

	int lst = listen(socket_fd,5);
	if(lst<0)
	{
		Rufius_Motion::log(3,"Server Listen Socket Error");
		stop();
		return;
	}

	struct sockaddr_un client_name;
	socklen_t client_name_length = sizeof(client_name);
	int client_socket_fd;

	int uid = 0;
	int message = 0;
	int message_extra = 0;
	
	while(Rufius_Motion::isRunning())
	{
		Rufius_Motion::log(9,"Server Looping");
		client_socket_fd = accept(Rufius_Daemon::socket_fd,(sockaddr*)&client_name,&client_name_length);

		if(client_socket_fd<0)
		{
			Rufius_Motion::log(3,"Server Client Socket Error");
		}
		else
		{
			if(read(client_socket_fd,&uid,sizeof(int)))
			{
				read(client_socket_fd,&message,sizeof(int));
				
				if(message&Rufius::MSGEXTRA)
				{
					read(client_socket_fd,&message_extra,sizeof(int));
				}
				
				Rufius_Daemon::handleMessage(message, uid);
				
				int outStatus = Rufius_Motion::getStatus();
				
				if(uid==0)
				{
					outStatus = outStatus | 0x100;
				}
				
				std::unordered_map<int,Rufius_User*>::const_iterator got = Rufius::users.find(uid);
				
				if(got!=Rufius::users.end())
				{
					if(Rufius::users_logined.find(uid)!=Rufius::users_logined.end())
					{
						outStatus = outStatus | 0x800;
					}
					
					if((got->second)->isInformed())
					{
						outStatus = outStatus | 0x400;
					}
					
					if((got->second)->isSuper())
					{
						outStatus = outStatus | 0x200;
					}
				}
				
				write(client_socket_fd,&outStatus,sizeof(int));

			}

			close(client_socket_fd);
		}
	}

	Rufius_Motion::log(9,"Server Loop Exit");
	
	close(socket_fd);
	unlink(rufius_socket);
}

void Rufius_Daemon::getIP_task()
{
	prctl(PR_SET_NAME,"rufius-getIP",0,0,0);
	while(Rufius_Motion::isRunning())
	{
		/*if(Rufius::debug_flag)
		{
			std::cout<<"[0]Getting IP...\n";
		}
		CURL *curl_handle;


		curl_handle = curl_easy_init();
		curl_easy_setopt(curl_handle, CURLOPT_URL, "ifconfig.me/ip");
		curl_easy_setopt(curl_handle, CURLOPT_NOPROGRESS, 1L);
		curl_easy_setopt(curl_handle, CURLOPT_WRITEFUNCTION, write_data);

		curl_easy_perform(curl_handle);

		curl_easy_cleanup(curl_handle);*/

		ldns_resolver* res = (ldns_resolver*)0;
		ldns_rdf* domain;
		ldns_pkt* p;
		ldns_rr_list* lp;
		ldns_status s = LDNS_STATUS_ERR;

		domain = ldns_dname_new_frm_str("myip.opendns.com");
		s = ldns_resolver_new_frm_file(&res,Rufius::nameserver_path);

		if(s==LDNS_STATUS_OK && domain!=(ldns_rdf*)0)
		{
			p = ldns_resolver_query(res, domain, LDNS_RR_TYPE_A, LDNS_RR_CLASS_IN, LDNS_RD);
			lp = ldns_pkt_rr_list_by_type(p, LDNS_RR_TYPE_A, LDNS_SECTION_ANSWER);
			ldns_rr_list_sort(lp);
			std::string stringIP(ldns_rr_list2str(lp));

			Rufius_Motion::log(2,"DNS Result :"+stringIP);

			if(stringIP.size()>0)
			{
				std::smatch cm;
				if(std::regex_search(stringIP,cm,Rufius::k_regex_ip))
				{
					std::string retrievedIP(cm[0].str());
					Rufius_Motion::log(2,"IP from myip.opendns.com :"+retrievedIP);
					if(Rufius::host_ip.compare(retrievedIP)!=0)
					{
						Rufius_Motion::log(2,"Renewing IP from "+Rufius::host_ip+" to "+retrievedIP+" and informing.");
						Rufius::host_ip = cm[0].str();
						
						if(informer_thread.joinable())
						{
							informer_thread.join();
						}
						
						informer_thread = std::thread(Rufius_Informer::inform_IP);
					}
				}
			}

			ldns_rr_list_deep_free(lp);
			ldns_rdf_deep_free(domain);
			ldns_pkt_free(p);
			ldns_resolver_deep_free(res);
		}
		else
		{
			Rufius_Motion::log(0,"LDNS ERROR :"+std::string(ldns_get_errorstr_by_id(s)));
		}

		if(Rufius_Motion::isRunning())
		{
			std::mutex mtx;
			std::unique_lock<std::mutex> lck(mtx);
			ip_cv.wait_for(lck,std::chrono::minutes(5));
		}
	}
}

void Rufius_Daemon::snapshotting_task()
{
	while(Rufius_Motion::isSnapshotting())
	{
		Rufius_Motion::takeSnapshot();
		
		if(Rufius_Motion::isRunning() && (!Rufius_Motion::isDetecting()))
		{
			std::mutex mtx;
			std::unique_lock<std::mutex> lck(mtx);
			sn_cv.wait_for(lck,std::chrono::seconds(Rufius_Motion::monitoring_interval));
		}
	}
}

void Rufius_Daemon::gracetime_task()
{
	prctl(PR_SET_NAME,"rufius-gracetime",0,0,0);
	
	Rufius_Motion::startGrace();
	
	std::mutex mtx;
	std::unique_lock<std::mutex> lck(mtx);
	sn_cv.wait_for(lck,std::chrono::seconds(Rufius::gracetime));
	
	if(Rufius_Motion::isRunning())
	{
		if(Rufius_Motion::monitoring_interval>0)
		{
			startMonitoring();
		}
		updateStatus();
	}
	
	Rufius_Motion::stopGrace();
}

size_t Rufius_Daemon::write_data(char *ptr, size_t size, size_t nmemb, void *stream)
{
	std::string data;
	for (size_t c = 0; c<size*nmemb; c++)
	{
		data.push_back(ptr[c]);
	}

	data.erase(std::remove(data.begin(), data.end(), '\n'), data.end());

	Rufius::host_ip_lock.lock();
	if(std::regex_match(data,Rufius::k_regex_ip))
	{
		if(Rufius::host_ip.compare(data))
		{
			Rufius::host_ip = data;

			Rufius_Informer::inform_IP();
		}
	}
	
	Rufius::host_ip_lock.unlock();

	return size*nmemb;
}

void Rufius_Daemon::startMonitoring()
{
	if((~(Rufius_Motion::getStatus() & (Rufius_Motion::MOTIONRUN))))
	{
		if((Rufius_Motion::monitoring_interval>=30) && (!Rufius_Motion::isSnapshotting()))
		{
			if(snapshotting_thread.joinable())
			{
				snapshotting_thread.join();
			}
			
			Rufius_Motion::startSnapshotting();
			
			snapshotting_thread = std::thread(snapshotting_task);
		}
		else if(Rufius_Motion::monitoring_interval>0)
		{
			Rufius_Motion::startMonitoring();
		}
	}
	
}

void Rufius_Daemon::stopMonitoring()
{
	if((Rufius_Motion::isSnapshotting()))
	{
		Rufius_Motion::stopSnapshotting();
		
		sn_cv.notify_all();
	}
	else if(Rufius_Motion::isMonitoring())
	{
		Rufius_Motion::stopMonitoring();
	}
}

bool Rufius_Daemon::handleMessage(int msg, int uid)
{
	std::unordered_map<int,Rufius_User*>::const_iterator got = Rufius::users.find(uid);
	bool root = uid==0;
	bool out = (got!=Rufius::users.end());
	bool super = out && (got->second)->isSuper();
	
	if(out || root)
	{
		switch(msg)
		{
			case Rufius::STATUS :
				break;
				
			case Rufius::INFON :
				if(out)
				{
					(got->second)->setInforming(true);
				}
				break;
				
			case Rufius::INFOFF :
				if(out)
				{
					(got->second)->setInforming(false);
				}
				break;
				
			case Rufius::INWIFI :
				if(out)
				{
					Rufius::users_logined.insert(uid);
					Rufius::users_fencined.insert(uid);
				}
				break;
				
			case Rufius::OUTWIFI :
				if(out)
				{
					Rufius::users_logined.erase(uid);
				}
				break;
				
			case Rufius::INFENCE :
				if(out)
				{
					Rufius::users_fencined.insert(uid);
				}
				break;
				
			case Rufius::OUTFENCE :
				if(out)
				{
					Rufius::users_fencined.erase(uid);
					Rufius::users_logined.erase(uid);
				}
				break;
				
			case Rufius::DETON :
				if(super || root)
				{
					Rufius_Motion::startDetection();
				}
				break;
				
			case Rufius::DETOFF :
				if(super || root)
				{
					Rufius_Motion::stopDetection();
				}
				break;
				
			case Rufius::SNAP :
				if(super || root)
				{
					Rufius_Motion::takeSnapshot();
				}
				break;
				
			case Rufius::SNAPON :
				if(super || root)
				{
					startMonitoring();
				}
				break;
				
			case Rufius::SNAPOFF :
				if(super || root)
				{
					stopMonitoring();
				}
				break;
				
			case Rufius::CAMON :
				if(super || root)
				{
					startMonitoring();
				}
				break;
				
			case Rufius::CAMOFF :
				if(super || root)
				{
					stopMonitoring();
				}
				break;
				
			case Rufius::TOGGLE :
				if(super || root)
				{
					Rufius_Motion::toggleDetection();
				}
				break;
				
			default:
				break;
		}
	}

	if(out && (!Rufius_Motion::isInGraceTime()))
	{
		updateStatus();
	}
	
	return out || root;
}

void Rufius_Daemon::signalhandler(int signo)
{
	switch(signo)
	{
		case SIGTERM :
			stop();
			break;
	}
}
