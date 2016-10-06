#include "Rufius_User.h"

Rufius_User::Rufius_User()
{
	name = (char*)0;
	id = -1;
	email = (char*)0;
	info = false;
	super = false;
}

Rufius_User::Rufius_User(uid_t i, const char* nm)
{
	id = i;
	
	int len = strlen(nm);
	if(len>0)
	{
		name = new char[len+1];
		strcpy(name,nm);
	}
	else
	{
		name = (char*)0;
	}

	email = (char*)0;
	
	super = false;
	info = false;
}

Rufius_User::Rufius_User(uid_t i, const char* nm, const char* ml)
{
	id = i;
	
	int len = strlen(nm);
	if(len>0)
	{
		name = new char[len+1];
		strcpy(name,nm);
	}
	else
	{
		name = (char*)0;
	}

	len = strlen(ml);
	if(len>0)
	{
		email = new char[len+1];
		strcpy(email,ml);
	}
	else
	{
		email = (char*)0;
	}


	super = false;
	info = true;
}

Rufius_User::Rufius_User(uid_t i, const char* nm, const char* ml, const bool su)
{
	id = i;
	
	int len = strlen(nm);
	if(len>0)
	{
		name = new char[len+1];
		strcpy(name,nm);
	}
	else
	{
		name = (char*)0;
	}

	len = strlen(ml);
	if(len>0)
	{
		email = new char[len+1];
		strcpy(email,ml);
	}
	else
	{
		email = (char*)0;
	}

	super = su;
	info = true;
}

Rufius_User::~Rufius_User()
{
	if(name)
	{
		delete[] name;
	}
	
	if(email)
	{
		delete[] email;
	}
}

void Rufius_User::setSuper(bool bl)
{
	super = bl;
}

void Rufius_User::setInforming(bool bl)
{
	info = bl;
}

void Rufius_User::setEmail(const char* ml)
{	
	int len = strlen(ml);
	if(ml && (len>0))
	{
		if(email)
		{
			delete[] email;
		}
		
		email = new char[len+1];
		strcpy(email,ml);
	}
}

const char* Rufius_User::getName()
{
	return name;
}

int Rufius_User::getId()
{
	return id;
}

const char* Rufius_User::getEmail()
{
	return email;
}

bool Rufius_User::isSuper()
{
	return super;
}

bool Rufius_User::isInformed()
{
	return info;
}