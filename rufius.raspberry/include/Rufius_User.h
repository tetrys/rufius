#ifndef RUFIUS_USER
#define RUFIUS_USER

#include <pwd.h>
#include <cstring>
#include <iostream>

class Rufius_User
{
public:
	Rufius_User();
	Rufius_User(uid_t i, const char* nm);
	Rufius_User(uid_t i, const char* nm, const char* ml);
	Rufius_User(uid_t i, const char* nm, const char* ml, const bool su);

	~Rufius_User();

	void setSuper(bool bl);
	void setInforming(bool bl);
	void setEmail(const char* ml);

	int getId();
	const char* getName();
	const char* getEmail();
	bool isSuper();
	bool isInformed();

	//Variables
private:
	char* name;
	uid_t id;
	char* email;
	bool super;
	bool info;
};

#endif // RUFIUS_USER

