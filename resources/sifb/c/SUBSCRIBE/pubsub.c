#include "pubsub.h"
#ifndef _MSC_VER
	#include <fcntl.h>
#endif

/*
http://www.kegel.com/dkftpbench/nonblocking.html
*/
#ifdef _MSC_VER
	int setNonblocking(SOCKET fd)
	{
		u_long flags;
#else
	int setNonblocking(int fd)
	{
		int flags;
#endif


		/* If they have O_NONBLOCK, use the Posix way to do it */
	#ifndef _MSC_VER
		/* Fixme: O_NONBLOCK is defined but broken on SunOS 4.1.x and AIX 3.2.5. */
		if (-1 == (flags = fcntl(fd, F_GETFL, 0)))
			flags = 0;
		return fcntl(fd, F_SETFL, flags | O_NONBLOCK);
	#else
		/* Otherwise, use the old way of doing it */
		flags = 1;
		return ioctlsocket(fd, FIONBIO, &flags);
	#endif
	} 
