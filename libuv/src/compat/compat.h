#ifndef _LIBUV_COMPAT_H
#define _LIBUV_COMPAT_H

#include <errno.h>

#if defined __ANDROID__ &&  __ANDROID_API__ < 24
#define LIBUV_EMBEDDED_PREADV
#define LIBUV_EMBEDDED_GETIFADDR
#define LIBUV_REPLACE_uv_interface_addresses
#endif

#ifdef LIBUV_EMBEDDED_PREADV
extern int preadv(int a0, void *a1, int a2, int a3);
extern int pwritev(int a0, void *a1, int a2, int a3);
#endif

#ifdef LIBUV_EMBEDDED_GETIFADDR

struct ifaddrs {
	struct ifaddrs  *ifa_next;
	char		*ifa_name;
	unsigned int	 ifa_flags;
	struct sockaddr	*ifa_addr;
	struct sockaddr	*ifa_netmask;
	struct sockaddr	*ifa_dstaddr;
	void		*ifa_data;
};

/*
 * This may have been defined in <net/if.h>.  Note that if <net/if.h> is
 * to be included it must be included before this header file.
 */
#ifndef	ifa_broadaddr
#define	ifa_broadaddr	ifa_dstaddr	/* broadcast address interface */
#endif

#include <sys/cdefs.h>

__BEGIN_DECLS
extern int getifaddrs(struct ifaddrs **ifap);
extern void freeifaddrs(struct ifaddrs *ifa);
__END_DECLS


#endif

#ifdef LIBUV_REPLACE_uv_interface_addresses
#define uv_interface_addresses uv_interface_addresses_original
#endif



#endif