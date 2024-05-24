#ifndef _NXTCOMMUNICATION_

#define _NXTCOMMUNICATION_

#include "pubsub.h"
#include "fbtypes.h"

#ifdef _MSC_VER
#include <WinSock2.h>
#endif

#ifndef IP_MULTICAST_IF 
	/* 
	 * The following constants are taken from include/netinet/in.h
	 * in Berkeley Software Distribution version 4.4. Note that these 
	 * values *DIFFER* from the original values defined by Steve Deering 
	 * as described in "IP Multicast Extensions for 4.3BSD UNIX related 
	 * systems (MULTICAST 1.2 Release)". It describes the extensions 
	 * to BSD, SunOS and Ultrix to support multicasting, as specified
	 * by RFC-1112. 
	 */ 
	#define IP_MULTICAST_IF 9 /* set/get IP multicast interface */ 
	#define IP_MULTICAST_TTL 10 /* set/get IP multicast TTL */
	#define IP_MULTICAST_LOOP 11 /* set/get IP multicast loopback */ 
	#define IP_ADD_MEMBERSHIP 12 /* add (set) IP group membership */ 
	#define IP_DROP_MEMBERSHIP 13 /* drop (set) IP group membership */ 
	#define IP_DEFAULT_MULTICAST_TTL 1 
	#define IP_DEFAULT_MULTICAST_LOOP 1 
	#define IP_MAX_MEMBERSHIPS 20 
	/* The structure used to add and drop multicast addresses */ 
	typedef struct ip_mreq { 
	  struct in_addr imr_multiaddr; /* multicast group to join */
	  struct in_addr imr_interface; /* interface to join on */
	}IP_MREQ; 
#endif 

// Linked list for clients

 struct sock_el
    {
#ifdef _MSC_VER
	SOCKET sock;
#else
	int sock;
#endif
	struct timeval* lastactivity;
	struct sock_el *next;
    };

	typedef struct sock_el SocketLinkedList;

 struct buff_el
    {
	char *buffer;
	int *buflen;
	struct buff_el *next;
    };

	typedef struct buff_el BufferLinkedList;

	typedef struct
	{
		unsigned int* events; // pointer to the block events
		char eventNum; //0+
	}OutputEvent;
	
	


	typedef enum
    {
        IEC_ANY = 0,

        IEC_BOOL = 1,
        IEC_SINT = 2,
        IEC_INT = 3,
        IEC_DINT = 4,
        IEC_LINT = 5,
        IEC_USINT = 6,
        IEC_UINT = 7,
        IEC_UDINT = 8,
        IEC_ULINT = 9,
        IEC_REAL = 10,
        IEC_LREAL = 11,
        IEC_TIME = 12,
        IEC_DATE = 13,
        IEC_TIME_OF_DAY = 14,
        IEC_DATE_AND_TIME = 15,
        IEC_STRING = 16,
        IEC_BYTE = 17,
        IEC_WORD = 18,
        IEC_DWORD = 19,
        IEC_LWORD = 20,
        IEC_WSTRING = 21,

        IEC_DerivedData = 26,
        IEC_DirectlyDerivedData = 27,
        IEC_EnumeratedData = 28,
        IEC_SubrangeData = 29,
        IEC_ARRAY = 22,
        IEC_STRUCT = 31
    }IECDataType;
	
	enum ASN1TagClass
    {
        IEC_UNIVERSAL = 0,
        IEC_APPLICATION = 64,
        IEC_CONTEXT = 128,
        IEC_PRIVATE = 192
    };

    enum EASN1EncodingType 
    {
        IEC_PRIMITIVE = 0,
        IEC_CONSTRUCTED = 32
    };

	typedef struct {
		IECDataType dataType; // IECDataType
		char data[64]; // max data size = 64 :(
		// WAS (same as new type: IECValuePtr);
		//void* data;
		int len; // only NEEDED for strings
	}IECValue;

	typedef struct {
		IECDataType dataType; // IECDataType
		void* data;
		int len; // only NEEDED for strings
	}IECValuePtr;


// Struct for protocol messages
typedef struct {
	unsigned short protocolversion;
	char source[32];
	unsigned short command;
	unsigned long long channelid;
	//Message Data

} NxtCommunicationMsgHeader;


// MessageTypes
	// Keepalive 
		// command == 0x01
		// has no data
	// Basic/Composite/Service Link Publisher
		// command == 0x82
		// unsigned long long EventId
		// unsigned int Reserved (send 0)
		// repeat:
			// unsigned long long DataID#
			// <ANY> Data#
	// Adapter Link
		// command == 0x87
		// unsigned long long ConnectionID
		// unsigned int Reserved (send 0)
		// bool IsPlug; // true = plug, false = socket
		// unsigned short EventNumber     #
		// send ALL DATA!!! "All adapter data is serialized, regardless of WITH."
		// repeat:
			// <ANY> Data#
			// values of 'null' is sent at 0x05

SocketLinkedList* getLastSock(SocketLinkedList *list);			

void printHex(unsigned char* buf, int offset, int len);

int decodeNxtPacketValue(unsigned char* packet, int o, IECValue* decodedValue);
int appendNxtPacketValue(unsigned char* output, int o, IECDataType type, void* value);
unsigned int fixInputData(unsigned char* packet, int offset, int datalen);


// Globals
char sourceName[32];


#endif
