#include "Rufius_Main.h"

int main(int argc, char *argv[])
{
	Rufius_Main::parseArguments(argc,argv);
	Rufius_Main::init();

	exit(EXIT_SUCCESS);
}