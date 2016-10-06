#ifndef RUFIUS
#define RUFIUS

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <grp.h>
#include <iostream>
#include <fstream>
#include <mutex>
#include <regex>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <openssl/ssl.h>
#include <termios.h>
#include "config.h"
#include "Rufius_User.h"

class Rufius
{

public:
	static int createUsersFromSystem();
	static int parseConf();
	static int checkConf();
	static int checkFiles();
	static int decryptPassword();
	static int passwordCallback(char *buf, int size, int rwflag, void *u);
	
	static void createImagesList(char* dirPath);
	
	static void setEmail(const char* cfg);
	static void setMailserver(const char* cfg);
	static void setConfiguration(const char* cfg);
	static void setPassword(const char* cfg);
	static void setRSAKey(const char* cfg);
	static void setNameserver(const char* cfg);
	static void setLog(const char* cfg);
	static void setGraceTime(std::string tm);
	static void setSnapshotsInterval(std::string tm);
	static void setMonitoringInterval(std::string tm);
	
	static void destroy();
	
	static void logErrors(int ercd);
	
	//Variables
public:
	enum MSGCODE {EMPTY=0, STATUS=1, INFON=2, INFOFF=3, INWIFI=4, OUTWIFI=5, INFENCE=6, OUTFENCE=7,
		DETON=8, DETOFF=9, SNAP=10, SNAPON=11, SNAPOFF=12, CAMON=13, CAMOFF=14, TOGGLE=15,
		MSGEXTRA=16
	};

	const static std::string k_ip_0_255;
	const static std::regex k_regex_ip;
	const static std::regex k_regex_rootmail_br;
	const static std::regex k_regex_gmail;
	const static std::regex k_regex_gmail_br;
	const static std::regex k_regex_gmail_IP;
	const static std::regex k_regex_smtp;
	const static std::regex k_regex_server_id;
	const static std::regex k_regex_motion_path;
	const static std::regex k_regex_snapshots_path;
	const static std::regex k_regex_snapshots_interval;
	const static std::regex k_regex_monitoring_interval;
	const static std::regex k_regex_triggers_path;
	const static std::regex k_regex_key_path;
	const static std::regex k_regex_pass_path;
	const static std::regex k_regex_nameserver_path;
	const static std::regex k_regex_log_path;
	const static std::regex k_regex_user;
	const static std::regex k_regex_time;
	
	static char* email;
	static char* mailserver;
	static char* passwort;
	static char* conf_path;
	static char* password_path;
	static char* rsakey_path;
	static char* nameserver_path;
	static char* log_path;
	static char* server_id;
		
	static std::string host_ip;
	static std::mutex host_ip_lock;

	static std::unordered_map<int,Rufius_User*> users;
	static std::unordered_set<int> users_logined;
	static std::unordered_set<int> users_fencined;
	
	static int gracetime;
};

#endif // RUFIUS

