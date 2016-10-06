#ifndef RUFIUSIP_H
#define RUFIUSIP_H

#include <sys/socket.h>
#include <sys/un.h>
#include <sys/prctl.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <cctype>
#include <thread>
#include <condition_variable>
#include <ldns/ldns.h>
#include "motion.h"
#include "Rufius.h"
#include "Rufius_Motion.h"

class Rufius_Daemon
{

public:
	static void instance();

private:
	static void prepare();
	static void init();
	static void start();
	static void stop();
	
	static void changeStatus();
	static void updateStatus();

	static void server_task();
	static void getIP_task();
	static void snapshotting_task();
	static void gracetime_task();
	
	static void setGraceTime(std::string tm);
	static void startMonitoring();
	static void stopMonitoring();
	
	static void signalhandler(int signo);

	static bool handleMessage(int msg, int uid);
	static size_t write_data(char *ptr, size_t size, size_t nmemb, void *stream);

	//Variables
private:

	static pid_t pid, sid;
	static int socket_fd;
	
	static std::condition_variable main_cv;
	static std::condition_variable ip_cv;
	static std::condition_variable sn_cv;
	static std::condition_variable gt_cv;
	static std::condition_variable if_cv;

	static std::thread getIP_thread;
	static std::thread server_thread;
	static std::thread snapshotting_thread;
	static std::thread gracetime_thread;
	static std::thread informer_thread;
};

#endif // RUFIUSIP_H
