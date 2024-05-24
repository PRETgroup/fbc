#ifndef ___NXTIECDATATYPES___
#define ___NXTIECDATATYPES___

#ifdef __cplusplus
extern "C"{
#endif

#ifdef _MSC_VER

#include <Winsock2.h>

#else

#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>

#endif

#include "NxtCommunication.h"

	struct str_el
    {
		char str[64]; // instead of char*
		bool inuse; // needed cause str may intensionally me empty :(
		struct str_el* next;
    };

	typedef struct str_el StringLinkedList;
	
	

	struct subscr_el
    {
		unsigned int linkid;
		unsigned int cookie;
	#ifdef _MSC_VER
		SOCKET* remotesocket;
	#else
		int* remotesocket;
	#endif
		struct subscr_el* next;
    };

	typedef struct subscr_el SubscriberList;

	/*typedef struct
	{
	#ifdef __WINDOWS__
		SOCKET* remotesockets;
	#else
		int* remotesockets;
	#endif
		unsigned int count;
	}PublisherList;*/

	typedef struct
	{
		SubscriberList subscribers;
		unsigned int* events; // pointer to the block events
		char eventNum;
		unsigned int dataCount;
		IECValuePtr* datain;
	}HMIInputEvent;

	typedef struct
	{
		unsigned int* events; // pointer to the block events
		char eventNum;
		unsigned int dataCount;
		IECValuePtr* dataout;
	}HMIOutputEvent;

	// Packets

	
    typedef struct
    {
		unsigned int requestID;
		unsigned short numberOfQueries;
		StringLinkedList names;
		StringLinkedList context;
    }NxtHMIAttributeQueryRequestMsg;

	enum NotifyNow {
		NOT_IMMEDIATE = 0,
		SEND_CURRENT = 1,
		GET_HISTORICAL_LIST = 2, // notfyParam = # of history
		GET_PAST_HISTORY = 3, // notfyParam = timestamp to start from
	};

	
    typedef struct
    {
		unsigned int requestID;
		char accessorPath[32];
		unsigned char eventNumber;	//bit 0-4 event# to sub to, starting with ev#0
									//bit 7 = 0 (input event (runtime->Canvas))
									//bit 7 = 1 (output event (Canvas->runtime))
		unsigned short cookie;		// arbitrary 16bit #
		unsigned char notifyNow;		//
		unsigned int notifyParam;	// param for notifyNow option 2/3
    }NxtHMISubscribeMsg;

	typedef struct
    {
		unsigned int requestID;
		char accessorPath[32];
		unsigned char eventNumber;	//bit 0-4 event#, starting with ev#0
									//bit 7 ALWAYS = 1 (output event (Canvas->runtime))
		time_t time; // TO Be Done by next control... send UDINT 0 for now
		IECValue data[16];
    }NxtHMISetDataPathMsg;



	// functions
	void appendToLinkedList(char* str, int len, StringLinkedList* listNode);
	void emptyStringLinkedList(StringLinkedList* list);

	void printLinkedList(StringLinkedList listNode);
	/*void newIECValue(IECValue** data);
	void freeIECValue(IECValue* data, bool andData);
		
	void freeNxtHMIAttributeQueryRequestMsg(NxtHMIAttributeQueryRequestMsg* decodedPacket);
	void freeNxtHMISubscribeMsg(NxtHMISubscribeMsg* decodedPacket);
	void freeNxtHMISetDataPathMsg(NxtHMISetDataPathMsg* decodedPacket, int dataCount);

	void tracefree(void* ptr);*/

#ifdef __cplusplus
}
#endif

#endif // ___NXTIECDATATYPES___
