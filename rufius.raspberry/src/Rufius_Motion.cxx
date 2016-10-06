#include "Rufius_Motion.h"
#include <iostream>

Rufius_Motion::function Rufius_Motion::on_detection_function = (Rufius_Motion::function)0;
Rufius_Motion::function Rufius_Motion::on_area_detection_function = (Rufius_Motion::function)0;
Rufius_Motion::function Rufius_Motion::on_event_start_function = (Rufius_Motion::function)0;
Rufius_Motion::function Rufius_Motion::on_event_end_function = (Rufius_Motion::function)0;
Rufius_Motion::function Rufius_Motion::on_camera_lost_function = (Rufius_Motion::function)0;
Rufius_Motion::function Rufius_Motion::on_picture_save_function = (Rufius_Motion::function)0;

int Rufius_Motion::verbosityLevel = -1;
int Rufius_Motion::snapshotting_interval = 0;
int Rufius_Motion::monitoring_interval = 0;

std::mutex Rufius_Motion::status_lock;

int Rufius_Motion::status = DOWN;

char* Rufius_Motion::motion_conf_path = (char*)0;
char* Rufius_Motion::snapshots_path = (char*)0;
char* Rufius_Motion::triggers_path = (char*)0;
char* Rufius_Motion::snapshots_list_path = (char*)0;
char* Rufius_Motion::triggers_list_path = (char*)0;

std::thread Rufius_Motion::motion_thread;

void Rufius_Motion::destroy()
{
	if(motion_thread.joinable())
	{
		motion_thread.join();
	}
	
	if(motion_conf_path)
	{
		delete[] motion_conf_path;
	}
	
	if(snapshots_path)
	{
		delete[] snapshots_path;
	}
	
	if(triggers_path)
	{
		delete[] triggers_path;
	}
	
	if(snapshots_list_path)
	{
		delete[] snapshots_list_path;
	}
	
	if(triggers_list_path)
	{
		delete[] triggers_list_path;
	}
}

void Rufius_Motion::motion_task()
{
	prctl(PR_SET_NAME,"rufius-motion",0,0,0);
	
	char* arg;
	char** arg_ptr = new char*[2];
	int argc = 0;
	if(motion_conf_path!=(char*)0)
	{
		arg = new char[strlen(motion_conf_path)+4];
		strcpy(arg,"-c ");
		strcat(arg,motion_conf_path);
		arg_ptr[argc] = arg;
		argc++;
	}
	
	if(verbosityLevel>0)
	{
		arg = new char[5];
		sprintf(arg,"-d %d",verbosityLevel);
		arg_ptr[argc] = arg;
		argc++;
	}
	
	motion(argc,arg_ptr);
	
	if(arg!=(char*)0)
	{
		delete arg;
	}
	
	delete[] arg_ptr;
}

void Rufius_Motion::updateMotion()
{
	int st = getStatus();
	
	if( (st & (DETECTING | MONITORING | SNAP)) && (!(st & MOTIONRUN)))
	{
		if(motion_thread.joinable())
		{
			motion_thread.join();
		}
		
		status_lock.lock();
		status = status | MOTIONRUN;
		status_lock.unlock();		
		motion_thread = std::thread(motion_task);
	}
	else if((st & MOTIONRUN) && (!(st & (DETECTING | MONITORING))))
	{
		status_lock.lock();
		status = status & (~MOTIONRUN);
		status_lock.unlock();
		
		motion_sig_handler(SIGTERM);
	}
}

void Rufius_Motion::takeSnapshot()
{
	status_lock.lock();
	status = status | SNAP;
	status_lock.unlock();
	
	updateMotion();
}

void Rufius_Motion::endSnapshot()
{
	status_lock.lock();
	status = status &(~SNAP);
	status_lock.unlock();
	
	updateMotion();
}

void Rufius_Motion::start()
{
	status_lock.lock();
	status = status | UP;
	status_lock.unlock();
	
	updateMotion();
}

void Rufius_Motion::stop()
{
	status_lock.lock();
	status = status & (~(UP | DETECTING | MONITORING | SNAP | SNAPSHOTTING));
	status_lock.unlock();
	
	updateMotion();
}

void Rufius_Motion::startDetection()
{
	status_lock.lock();
	status = status | DETECTING;
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::stopDetection()
{
	status_lock.lock();
	status = status & (~DETECTING);
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::toggleDetection()
{
	status_lock.lock();
	status = status ^ DETECTING;
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::startMonitoring()
{
	status_lock.lock();
	status = status | MONITORING;
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::stopMonitoring()
{
	status_lock.lock();
	status = status & (~MONITORING);
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::startSnapshotting()
{
	status_lock.lock();
	status = status | SNAPSHOTTING;
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::stopSnapshotting()
{
	status_lock.lock();
	status = status & (~SNAPSHOTTING);
	status_lock.unlock();
	updateMotion();
}

void Rufius_Motion::startGrace()
{
	status_lock.lock();
	status = status | INGRACETIME;
	status_lock.unlock();
}

void Rufius_Motion::stopGrace()
{
	status_lock.lock();
	status = status & (~INGRACETIME);
	status_lock.unlock();
}

void Rufius_Motion::setConfiguration(const char* cfg)
{
	motion_conf_path = new char[strlen(cfg)+1];
	strcpy(motion_conf_path,cfg);
}

void Rufius_Motion::setSnapshots(const char* cfg)
{
	snapshots_path = new char[strlen(cfg)+1];
	strcpy(snapshots_path,cfg);
}

void Rufius_Motion::setTriggers(const char* cfg)
{
	triggers_path = new char[strlen(cfg)+1];
	strcpy(triggers_path,cfg);
}

void Rufius_Motion::setSnapshotsList()
{
	snapshots_list_path = new char[strlen(snapshots_path)+5];
	strcpy(snapshots_list_path,snapshots_path);
	strcat(snapshots_list_path,"/list");
}

void Rufius_Motion::setTriggersList()
{
	triggers_list_path = new char[strlen(triggers_path)+5];
	strcpy(triggers_list_path,triggers_path);
	strcat(triggers_list_path,"/list");
}

void Rufius_Motion::setMonitoringInterval(int in)
{
	if((~(Rufius_Motion::getStatus() & (Rufius_Motion::MOTIONRUN))))
	{
		Rufius_Motion::monitoring_interval = in;
	}
}

void Rufius_Motion::setSnapshottingInterval(int in)
{
	if((~(Rufius_Motion::getStatus() & (Rufius_Motion::MOTIONRUN))))
	{
		Rufius_Motion::snapshotting_interval = in;
	}
}

char* Rufius_Motion::getConfiguration()
{
	return motion_conf_path;
}

char* Rufius_Motion::getSnapshots()
{
	return snapshots_path;
}

char* Rufius_Motion::getTriggers()
{
	return triggers_path;
}

char* Rufius_Motion::getSnapshotsList()
{
	return snapshots_list_path;
}

char* Rufius_Motion::getTriggersList()
{
	return triggers_list_path;
}

int Rufius_Motion::getStatus()
{
	int out = 0;
	
	status_lock.lock();
	out = status;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isRunning()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&UP;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isDetecting()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&DETECTING;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isMonitoring()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&MONITORING;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isSnapshotting()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&SNAPSHOTTING;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isMotionRunning()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&MOTIONRUN;
	status_lock.unlock();
	
	return out;
}

bool Rufius_Motion::isInGraceTime()
{
	boolean out = false;
	
	status_lock.lock();
	out = status&INGRACETIME;
	status_lock.unlock();
	
	return out;
}

void Rufius_Motion::on_detection()
{
	if(on_detection_function)
	{
		on_detection_function();
	}
}

void Rufius_Motion::on_area_detection()
{
	if(on_area_detection_function)
	{
		on_area_detection_function();
	}
}

void Rufius_Motion::on_event_start()
{
	if(on_event_start_function)
	{
		on_event_start_function();
	}
}

void Rufius_Motion::on_event_end()
{
	if(on_event_end_function)
	{
		on_event_end_function();
	}
}

void Rufius_Motion::on_camera_lost()
{
	if(on_camera_lost_function)
	{
		on_camera_lost_function();
	}
}

void Rufius_Motion::on_picture_save()
{
	if(on_picture_save_function)
	{
		on_picture_save_function();
	}
}

void Rufius_Motion::logSnapshotsList(char* imageFile)
{
	struct stat buf;
	
	if(stat(snapshots_list_path,&buf)==0)
	{
		std::ofstream output(snapshots_list_path,std::ios::app);
		
		if(output.is_open())
		{
			output<<imageFile;
			output<<"\n";
			
			output.close();
		}
	}
}

void Rufius_Motion::logTriggersList(char* imageFile)
{
	struct stat buf;
	
	if(stat(triggers_list_path,&buf)==0)
	{
		std::ofstream output(triggers_list_path,std::ios::app);
		
		if(output.is_open())
		{
			output<<imageFile;
			output<<"\n";
			
			output.close();
		}
	}
}

void Rufius_Motion::log(int level, const char *msg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<'\n';
	}
}

void Rufius_Motion::log(int level, std::string msg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<'\n';
	}
}

void Rufius_Motion::log(int level, const char *msg, int arg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<' '<<arg<<'\n';
	}
}

void Rufius_Motion::log(int level, std::string msg, int arg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<' '<<arg<<'\n';
	}
}

void Rufius_Motion::log(int level, const char *msg, const char* arg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<' '<<arg<<'\n';
	}
}

void Rufius_Motion::log(int level, std::string msg, const char* arg)
{
	if(level<=verbosityLevel)
	{
		std::cout<<'['<<level<<"] "<<msg<<' '<<arg<<'\n';
	}
}
