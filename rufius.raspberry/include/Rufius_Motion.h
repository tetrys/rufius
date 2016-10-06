#ifndef RUFIUS_MOTION
#define RUFIUS_MOTION

#include <sys/prctl.h>
#include <mutex>
#include <thread>
#include <iostream>
#include <fstream>
#include "motion.h"

class Rufius_Motion
{
public:
	typedef void(*function)();
	
	enum CODE_STATUS{DOWN=0x00, UP=0x01, MOTIONRUN = 0x02, MONITORING=0x04, DETECTING=0x08, SNAP=0x10, SNAPSHOTTING=0x20, INGRACETIME=0x40};
	
	static void destroy();
	
	static void motion_task();
	
	static void updateMotion();
	static void takeSnapshot();
	static void endSnapshot();
	
	static void start();
	static void stop();
	static void startMonitoring();
	static void stopMonitoring();
	static void startSnapshotting();
	static void stopSnapshotting();
	static void startDetection();
	static void stopDetection();
	static void toggleDetection();
	static void startGrace();
	static void stopGrace();
	
	static void setConfiguration(const char* cfg);
	static void setSnapshots(const char* cfg);
	static void setTriggers(const char* cfg);
	static void setSnapshotsList();
	static void setTriggersList();
	
	static void setMonitoringInterval(int in);
	static void setSnapshottingInterval(int in);
	
	static char* getConfiguration();
	static char* getSnapshots();
	static char* getTriggers();
	static char* getSnapshotsList();
	static char* getTriggersList();
	
	static int getStatus();
	
	static bool isRunning();
	static bool isDetecting();
	static bool isMonitoring();
	static bool isSnapshotting();
	static bool isMotionRunning();
	static bool isInGraceTime();
	
	static void on_detection();
	static void on_area_detection();
	static void on_event_start();
	static void on_event_end();
	static void on_camera_lost();
	static void on_picture_save();
	
	static void logSnapshotsList(char* imageFile);
	static void logTriggersList(char* imageFile);
	
	static void log(int level, const char* msg);
	static void log(int level, std::string msg);

	static void log(int level, const char* msg, int arg);
	static void log(int level, std::string msg, int arg);
	
	static void log(int level, const char* msg, const char* arg);
	static void log(int level, std::string msg, const char* arg);
	
	static function on_detection_function;
	static function on_area_detection_function;
	static function on_event_start_function;
	static function on_event_end_function;
	static function on_camera_lost_function;
	static function on_picture_save_function;
	
	//Variables
private:
	
	static int status;
	static std::mutex status_lock;
	
	static std::thread motion_thread;
	
	static char* motion_conf_path;
	static char* snapshots_path;
	static char* triggers_path;
	static char* snapshots_list_path;
	static char* triggers_list_path;
	
public:	
	static int verbosityLevel;
	static int snapshotting_interval;
	static int monitoring_interval;
};

#endif //RUFIUS_MOTION