#ifndef RUFIUS_MAIN
#define RUFIUS_MAIN

#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <fcntl.h>
#include <grp.h>
#include <unistd.h>
#include <stdio.h>
#include <cstdio>
#include <iostream>
#include <iomanip>
#include <string>
#include <mutex>
#include <queue>
#include <map>
#include <utility>
#include <fstream>
#include <regex>
#include "Rufius_Daemon.h"

class Rufius_Main
{
public:

	static void parseArguments(int argc, char* argv[]);
	static unsigned int requiresVariable(unsigned int type);
	
	static void checkEmail(char* msg);
	static int checkMessage(char* msg);
	static void checkVerbosity(char *msg);
	static void checkPath(char* msg,int type);
	static int parseNumber(char* msg);
	static void printUsage();
	static bool isRunning();

	static void init();
	static void destroy();

	static void startServer();
	static void clearServer();
	static void sendMessage();

private:
	static int createFiles();

	//Variables
private:
	static unsigned int config;
	static int message;
	static int messageExtra;
};

#endif // RUFIUS_MAIN

