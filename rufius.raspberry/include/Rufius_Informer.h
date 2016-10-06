#ifndef RUFIUS_INFORMER
#define RUFIUS_INFORMER

#include "Rufius.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <curl/curl.h>

class Rufius_Informer
{

public:

	static void init();
	static void destroy();

	static void inform_init();
	static void inform_IP();
	static void on_detection();
	static void on_area_detection();
	static void on_event_start();
	static void on_event_end();
	static void on_camera_lost();
	static void on_picture_save();

private:

	static void write_date();
	static void write_mail(const char* usrmail);
	static void write_info_init(const char* usrmail);
	static void write_info_IP(const char* usrmail);
	static void write_info_detection(const char* usmail);
	static void write_info_camera(const char* usmail);
	static void inform_init_task(const char* usrmail);
	static void inform_IP_task(const char* usrmail);
	static void inform_detection_task(const char* usrmail);
	static void inform_camera_task(const char* usrmail);

	static size_t payload_source(void *ptr, size_t size, size_t nmemb, void *userp);

	//Variables
private:

	static bool inited;
	struct upload_status
	{
		int lines_read;
	};

	static const char k_wday_name[][4];
	static const char k_mon_name[][4];

	static std::thread* inform_init_thread;
	static std::thread* inform_IP_thread;
	static std::thread* inform_detection_thread;

	static std::mutex payload_lock;
	
	static char* serverEmail;
	
	static char** payload_text;
	
	static struct tm* event_time;
};

#endif // RUFIUS_INFORMER
