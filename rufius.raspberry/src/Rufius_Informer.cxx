#include "Rufius_Informer.h"
#include "Rufius.h"
#include <Rufius_Motion.h>

const char Rufius_Informer::k_wday_name[][4] =
{
	"Sun","Mon","Tue","Wed","Thu","Fri","Sat"
};

const char Rufius_Informer::k_mon_name[][4] =
{
	"Jan","Fab","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
};

bool Rufius_Informer::inited = false;

struct tm* Rufius_Informer::event_time = (tm*)0;

char** Rufius_Informer::payload_text = new char*[9];

char* Rufius_Informer::serverEmail = (char*)0;

std::thread* Rufius_Informer::inform_init_thread;
std::thread* Rufius_Informer::inform_IP_thread;
std::thread* Rufius_Informer::inform_detection_thread;

std::mutex Rufius_Informer::payload_lock;

void Rufius_Informer::init()
{
	Rufius_Motion::log(3,"Informer Initiation");
	if(Rufius::email)
	{
		int len = strlen(Rufius::email);
		serverEmail = new char[len+3];
		payload_text[0] = new char[33];
		payload_text[1] = new char[100];
		payload_text[2] = new char[len+11];
		payload_text[3] = new char[40];
		payload_text[4] = "Content-Type: text/plain\r\n";
		payload_text[5] = "\r\n";
		payload_text[6] = new char[21];
		payload_text[7] = new char[300];
		payload_text[8] = NULL;
		
		serverEmail[0] = '<';
		serverEmail[len+1] = '>';
		serverEmail[len+2] = '\0';
		
		payload_text[2][6] = '<';
		payload_text[2][len+7] = '>';
		payload_text[2][len+8] = '\r';
		payload_text[2][len+9] = '\n';
		payload_text[2][len+10] = '\0';
		
		strncpy(payload_text[2],"From: ",6);
		for(int i=0;i<len;i++)
		{
			serverEmail[i+1] = Rufius::email[i];
			payload_text[2][i+7] = Rufius::email[i];
		}
		
		
		Rufius_Informer::inform_init_thread = new std::thread[Rufius::users.size()];
		Rufius_Informer::inform_IP_thread = new std::thread[Rufius::users.size()];
		Rufius_Informer::inform_detection_thread = new std::thread[Rufius::users.size()];
		
		curl_global_init(CURL_GLOBAL_ALL);
		
		Rufius_Motion::log(3,"Informer Initiation Successful");
				
		inited = true;
	}
	else
	{
		Rufius_Motion::log(3,"Informer Initiation Failure");
	}
}

void Rufius_Informer::destroy()
{
	if(inited)
	{
		for(size_t i=0;i<Rufius::users.size();i++)
		{
			if(inform_init_thread[i].joinable())
			{
				inform_init_thread[i].join();
			}
			
			if(inform_IP_thread[i].joinable())
			{
				inform_IP_thread[i].join();
			}
			
			if(inform_detection_thread[i].joinable())
			{
				inform_detection_thread[i].join();
			}
		}
		
		delete[] inform_init_thread;
		delete[] inform_IP_thread;
		delete[] inform_detection_thread;
		
		delete[] payload_text[0];
		delete[] payload_text[1];
		delete[] payload_text[2];
		delete[] payload_text[3];
		//delete[] payload_text[4];
		//delete[] payload_text[5];
		delete[] payload_text[6];
		delete[] payload_text[7];
		//delete[] payload_text[8];
		
		delete[] serverEmail;
		delete[] payload_text;
		
		curl_global_cleanup();
	}
}

void Rufius_Informer::inform_init()
{
	int i=0;
	for(std::pair<int,Rufius_User*> x : Rufius::users)
	{
		Rufius_Motion::log(3,"Informing User",x.second->getName());
		
		const char* address = x.second->getEmail();
		
		if(address!=(char*)0)
		{
			Rufius_Motion::log(3,"User Email",address);
			
			if(Rufius_Informer::inform_init_thread[i].joinable())
			{
				Rufius_Informer::inform_init_thread[i].join();
			}
			Rufius_Informer::inform_init_thread[i] = std::thread(inform_init_task,address);
		}
		else
		{
			Rufius_Motion::log(3,"No Email for User",x.second->getName());
		}
		i++;
	}
}

void Rufius_Informer::inform_IP()
{
	int i=0;
	for(std::pair<int,Rufius_User*> x : Rufius::users)
	{
		Rufius_Motion::log(3,"Informing User",x.second->getName());
		
		const char*  address = x.second->getEmail();
		if(address!=(char*)0)
		{
			Rufius_Motion::log(3,"User Email",address);
			if(Rufius_Informer::inform_IP_thread[i].joinable())
			{
				Rufius_Informer::inform_IP_thread[i].join();
			}
			Rufius_Informer::inform_IP_thread[i] = std::thread(inform_IP_task,address);
		}
		else
		{
			Rufius_Motion::log(3,"No Email for User",x.second->getName());
		}
		i++;
	}
}

void Rufius_Informer::on_detection()
{
	
}

void Rufius_Informer::on_area_detection()
{

}

void Rufius_Informer::on_event_start()
{
	Rufius_Motion::log(0,"Event Started");
	int i=0;
	for(std::pair<int,Rufius_User*> x : Rufius::users)
	{
		const char* address = x.second->getEmail();
		if((address!=(char*)0) && (x.second->isInformed()))
		{
			if(Rufius_Informer::inform_detection_thread[i].joinable())
			{
				Rufius_Informer::inform_detection_thread[i].join();
			}
			Rufius_Informer::inform_detection_thread[i] = std::thread(inform_detection_task,address);
		}
		i++;
	}
}

void Rufius_Informer::on_event_end()
{

}

void Rufius_Informer::on_camera_lost()
{
	Rufius_Motion::log(0,"Camera Lost");
	int i=0;
	for(std::pair<int,Rufius_User*> x : Rufius::users)
	{
		const char* address = x.second->getEmail();
		if(address!=(char*)0)
		{
			if(Rufius_Informer::inform_init_thread[i].joinable())
			{
				Rufius_Informer::inform_init_thread[i].join();
			}
			Rufius_Informer::inform_init_thread[i] = std::thread(inform_camera_task,address);
		}
		i++;
	}
}

void Rufius_Informer::on_picture_save()
{

}

void Rufius_Informer::write_date()
{
	time_t rawtime;

	time(&rawtime);
	event_time = gmtime(&rawtime);

	sprintf(payload_text[0],"Date: %.3s %.3s%3d %.2d:%.2d:%.2d %.2d\r\n",
			Rufius_Informer::k_wday_name[Rufius_Informer::event_time->tm_wday],
			Rufius_Informer::k_mon_name[Rufius_Informer::event_time->tm_mon],
			event_time->tm_mday, event_time->tm_hour,
			event_time->tm_min, event_time->tm_sec,
			1900 + event_time->tm_year);
	sprintf(payload_text[6],"%.4d-%.2d-%.2d_%.2d:%.2d:%.2d ",
			1900 + event_time->tm_year,1 + event_time->tm_mon, event_time->tm_mday,
			event_time->tm_hour,event_time->tm_min, event_time->tm_sec);
}

void Rufius_Informer::write_mail(const char* usrmail)
{
	CURL *curl_handle;
	
	struct curl_slist *recipients = NULL;
	struct Rufius_Informer::upload_status upload_ctx;
	
	upload_ctx.lines_read = 0;
	
	curl_handle = curl_easy_init();
	if (curl_handle)
	{
		curl_easy_setopt(curl_handle, CURLOPT_URL, Rufius::mailserver);
		curl_easy_setopt(curl_handle, CURLOPT_USE_SSL, CURLUSESSL_ALL);
		curl_easy_setopt(curl_handle, CURLOPT_USERNAME, Rufius::email);
		curl_easy_setopt(curl_handle, CURLOPT_PASSWORD, Rufius::passwort);
		curl_easy_setopt(curl_handle, CURLOPT_MAIL_FROM, serverEmail);
		recipients = curl_slist_append(recipients, usrmail);
		curl_easy_setopt(curl_handle, CURLOPT_MAIL_RCPT, recipients);
		curl_easy_setopt(curl_handle, CURLOPT_READFUNCTION, payload_source);
		curl_easy_setopt(curl_handle, CURLOPT_READDATA, &upload_ctx);
		curl_easy_setopt(curl_handle, CURLOPT_UPLOAD, 1L);
				
		if(Rufius_Motion::verbosityLevel==9)
		{
			curl_easy_setopt(curl_handle, CURLOPT_VERBOSE, 1L);
		}
		
		CURLcode res = curl_easy_perform(curl_handle);
		
		if(res!=CURLE_OK)
		{
			Rufius_Motion::log(3,"CURL Error",res);
		}
		curl_slist_free_all(recipients);
		curl_easy_cleanup(curl_handle);
	}
}

void Rufius_Informer::write_info_init(const char* usrmail)
{
	write_date();

	snprintf(payload_text[1],100,"To: %s\r\n",usrmail);

	sprintf(payload_text[3],"Subject: Server Initialization.\r\n");
	sprintf(payload_text[7],"Server is up! Detection will start in %d sec.\nPlease declare your Presence or not.\r\n",Rufius::gracetime);
}

void Rufius_Informer::write_info_IP(const char* usrmail)
{
	write_date();
	snprintf(payload_text[1],100,"To: %s\r\n",usrmail);

	sprintf(payload_text[3],"Subject: IPChange\r\n");
	sprintf(payload_text[7],"%s%s\r\n",Rufius::server_id, Rufius::host_ip.c_str());
}

void Rufius_Informer::write_info_detection(const char* usrmail)
{
	write_date();

	snprintf(payload_text[1],100,"To: %s\r\n",usrmail);

	sprintf(payload_text[3],"Subject: Detection\r\n");
	sprintf(payload_text[7],"Motion Detected on %s%s.\r\n", Rufius::server_id ,Rufius::host_ip.c_str());
}

void Rufius_Informer::write_info_camera(const char* usrmail)
{
	write_date();
	
	snprintf(payload_text[1],100,"To: %s\r\n",usrmail);
	
	sprintf(payload_text[3],"Subject: Camera Lost.\r\n");
	sprintf(payload_text[7],"Camera Signal is Lost on %s%s.\r\n", Rufius::server_id ,Rufius::host_ip.c_str());
}

void Rufius_Informer::inform_init_task(const char *usrmail)
{
	prctl(PR_SET_NAME,"rufius-inform_init",0,0,0);
	Rufius_Motion::log(3,"Informing Init Task Initiation for ",usrmail);
	
	payload_lock.lock();
	write_info_init(usrmail);
	write_mail(usrmail);
	payload_lock.unlock();
}

void Rufius_Informer::inform_IP_task(const char* usrmail)
{
	prctl(PR_SET_NAME,"rufius-inform_ip",0,0,0);
	Rufius_Motion::log(3,"Informing IP Task Initiation for ",usrmail);
	
	payload_lock.lock();
	write_info_IP(usrmail);
	write_mail(usrmail);
	payload_lock.unlock();
}

void Rufius_Informer::inform_detection_task(const char* usrmail)
{
	prctl(PR_SET_NAME,"rufius-inform_detection",0,0,0);
	Rufius_Motion::log(3,"Informing Detection Task Initiation for ",usrmail);
	
	payload_lock.lock();
	write_info_detection(usrmail);
	write_mail(usrmail);
	payload_lock.unlock();
}

void Rufius_Informer::inform_camera_task(const char* usrmail)
{
	Rufius_Motion::log(3,"Informing Init Task Initiation for ",usrmail);
	
	payload_lock.lock();
	write_info_camera(usrmail);
	write_mail(usrmail);
	payload_lock.unlock();
}

size_t Rufius_Informer::payload_source(void *ptr, size_t size, size_t nmemb, void *userp)
{
	struct Rufius_Informer::upload_status *upload_ctx = (struct Rufius_Informer::upload_status *)userp;
	const char *data;

	if(((size == 0)||(nmemb == 0)||((size*nmemb) < 1)))
	{
		std::cout<<"Payload Source Failure\n";
		return 0;
	}

	data = payload_text[upload_ctx->lines_read];

	if(data)
	{
		size_t len = strlen(data);
		memcpy(ptr, data, len);
		upload_ctx->lines_read++;

		return len;
	}

	return 0;
}
