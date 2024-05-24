#ifndef _NXTHMISERVER__H_
#define _NXTHMISERVER__H_

#include "NxtHMIData.h"

    enum NxtHMIMsgType {
		SUBSCRIBE = 0x01, // H Subscribe for the notifications of an accessor link data. Will be replied-to by SUBSCRIBECNF.
		SUBSCRIBECNF = 0x81, // H Confirmation of a subscription
		NOTIFY = 0x02, // H Notification from an accessor link
		UNSUBSCRIBE = 0x03, // H Unsubscribe an accessor link. Will be replied-to by SUBSCRIBECNF.
		SETDATAPATH = 0x04, // H Set the data in an IEC runtime using an access path. Will be replied-to by SETDATACNF.
		SETDATALINKID = 0x05, // H Set the data in an IEC runtime using a link id. Will be replied-to by SETDATACNF (implemented later)
		SETDATACNF = 0x82, // H Confirmation of data setting.
		SUBWATCH = 0x06, // W Subscribe for watching
		UNSUBWATCH = 0x07, // W Watch unsubscribe
		SUBWATCHCNF = 0x83, // W Watch subscribe confirm
		WATCHNOTIFY = 0x08, // W Watch notification
		HISTORYRQ = 0x0b, // H History request
		HISTORYCNF = 0x86, // H History confirmation
		SUBSCRIBEALARM = 0x10, // H Subscribe to alarms. Will be replied-to by SUBSCRIBEALARMCNF.
		UNSUBSCRIBEALARM = 0x11, // H Unsubscribe alarms. Will be replied-to by SUBSCRIBEALARMCNF.
		SUBSCRIBEALARMCNF = 0x8b, // H Confirmation of an alarm subscription
		ALARMNOTIFY = 0x8c, // H Alarm notification
		ALARMHISTRQ = 0x12, // H Alarm history request
		ALARMHISTREPLY = 0x8d, // H Alarm history reply
		ALARMACK = 0x13, // H Alarm acknowledge
		ALARMACKCNF = 0x8e, // H Alarm acknowledge confirmation
		CONFIGRQST = 0x84, // D Configuration request
		CONFIGRSP = 0x09, // D Configuration response
		ATTRSETRQ = 0x0c, // W Attribute set request
		ATTRSETCNF = 0x87, // W Attribute set confirm
		ATTRDELRQ = 0x0d, // W Attribute delete request
		ATTRDELCNF = 0x88, // W Attribute delete confirm
		ATTRQUERYRQST = 0x14, // HWD Attribute query request
		ATTRQUERYRSP = 0x8f, // HWD Attribute query response
		LISTFORCERQST = 0x0e, // W List forces request
		LISTFORCECNF = 0x89, // W List forces response
		TRIGGEREVENTRQST = 0x0f, // W Trigger event request
		TRIGGEREVENTCNF = 0x8a, // W Trigger event confirm
		HEARTBEAT = 0x15, // H Heartbeat
		DIGISCOPERQST = 0x16, // H Digiscope request
		DIGISCOPERSP = 0x90, // H Digiscope response
		LISTEVBLOCKRQST = 0x17, // W List blocked events request
		LISTEVBLOCKRSP = 0x91, // W List blocked events reqponse
		SUBWATCHIDX = 0x1c, // W Watch subscribe to a particular index of an array
    };



	
	// functions
	unsigned int DATA_TO_UDINT(char* data);
	unsigned int getNewLinkID();
	
	int decodePacket(unsigned char* packet, int o, unsigned char* requestType, NxtHMIAttributeQueryRequestMsg* attReqMsg, NxtHMISubscribeMsg* subMsg, NxtHMISetDataPathMsg* setdatapathMsg);
	int startPacket(unsigned char* output, int offset);
	int endPacket(unsigned char* output, int offset);
	int endBadPacket(unsigned char* output, int offset);
	int handleAttrQueryRqst(unsigned char* output, NxtHMIAttributeQueryRequestMsg* req);
	
#ifdef _MSC_VER
	int handleSub(unsigned char* output, NxtHMISubscribeMsg* req, SOCKET* remotesocket);
#else
	int handleSub(unsigned char* output, NxtHMISubscribeMsg* req, int* remotesocket);
#endif
	int handleHeartBeatRsp(unsigned char* output);
	int handleSetDataPath(unsigned char* output, NxtHMISetDataPathMsg* req);
	int sendNotification(unsigned char* output, int o, unsigned int linkid, unsigned short cookie, int dataCount, IECValuePtr* data);
	int NxtHMIServerinit();
	void NxtHMIServerrun();
	void NxtHMIServerclose();
	

#endif
