#ifdef __cplusplus
extern "C"{
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

//#define __DEBUG__
#include "NxtHMIServer.h"

#ifdef _MSC_VER

#define WIN32_LEAN_AND_MEAN        /* define win 32 only */
#include <windows.h>
#include <Winsock2.h>
#include <Ws2tcpip.h>
extern void subscribeSocket(NxtHMISubscribeMsg* req, SOCKET* socket);
#else
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <fcntl.h>
extern void subscribeSocket(NxtHMISubscribeMsg* req, int* socket);
#endif
extern int setData(NxtHMISetDataPathMsg* req);
extern void processUpdates();



// Project.Name
extern char* ProjectName;// = "AirportModel";
extern char* ProjectState;// = "Running";
// GUID FROM <Project>\IEC61499\DatabaseDB.xml
extern char* ProjectGuid;// = "{8712C165-3E5F-4D48-BEB0-4B417700E6E8}";
extern unsigned short HMIServerPort;// Default = 61498



#ifdef _MSC_VER
		SOCKET hmiserversock;//, current_client;
		SOCKET clients[20];
#else
		int hmiserversock;//, current_client;
		int clients[20];
#endif
int clientCount = 0;


	unsigned int DATA_TO_UDINT(char* data)
	{
		unsigned int value = 0;
		memcpy(&value, data, 4);
		return value;
	}

	
	unsigned int getNewLinkID()
	{
		static int alinkid = 1;
		return alinkid++;
	}

	
	int decodePacket(unsigned char* packet, int o, unsigned char* requestType, NxtHMIAttributeQueryRequestMsg* attReqMsg, NxtHMISubscribeMsg* subMsg, NxtHMISetDataPathMsg* setdatapathMsg)
	{
		int offset = o;
		int valueCount = 0;
		int valueOffset = 0; // used when making PacketStruct
		
		int i;
		unsigned int requestID;
		IECValue decodedValues[16]; // hopefull no more than 16 at once
		
		offset += 4; //	be ee ee ef indicates the start of the message (skip it)
		//offset += 2; // 	46 14 // requestType (I Think)
		offset = decodeNxtPacketValue(packet, offset, &decodedValues[0]); //NxtHMIMsgType
		if( offset == -1 )
		{
			// read failed
			printHex(packet, o, 10);
			return -1;
		}
		//test = (char*)(tempValuePtr->data);
		*requestType = *((unsigned char*)decodedValues[0].data);
		//printf("requestType = 0x%02x\n", *requestType); //IEC_USINT
		
		if( *requestType == HEARTBEAT )
		{
			// no contents
			offset += 4; // Consume EOPacket
			return offset;
		}
		offset = decodeNxtPacketValue(packet, offset, &decodedValues[0]);
		requestID = *((unsigned int*)decodedValues[0].data);
		//printf("requestID = %d\n", requestID); //IEC_UDINT
		
		//offset = decodeNxtPacketValue(packet, offset, &tempValuePtr);
		//numberOfQueries = *((unsigned short*)tempValuePtr->data);
		//printf("numberOfQueries = %d\n", numberOfQueries); // IEC_UINT
		
        //IECStringValue.DecodePacket(packet, offset	, IECDataType.IEC_STRING, out value);
        //string name1 = ((IECStringValue)value).Value;
        //names.Add(name1);
            
		
		//newIECValue(&decodedValues[0]);
		while(1)
		{
			// 2 bytes for valueType
			int newoffset = decodeNxtPacketValue(packet, offset, &decodedValues[valueCount]);
			if( newoffset == -1 ) // END
			{
				if( packet[offset] == 0xBE && packet[offset+1] == 0xEE && packet[offset+2] == 0xEE && packet[offset+3] == 0xFF) // END OF PACKET
				{
					offset += 4; // Consume EOPacket
					//printf("End of packet found successfully\n");
				}
				else if( packet[offset] == 0xBE && packet[offset+1] == 0xEE && packet[offset+2] == 0xEE && packet[offset+3] == 0xAF) // END OF ABORTED PACKET
				{
					offset += 4; // Consume EOPacket
					printf("End of Aborted Packet Found Successfully\n");
					*requestType = 0xFF;
					printHex(packet, o, offset-o);
					return offset;
				}
				else
				{
					printf("Died on %02X %d @ %d\n", packet[offset] & 0xff, packet[offset], offset);
					return -1;
				}
				break;
			}

			offset = newoffset;

			valueCount++;
		}
		
		// Free decodedValues?
		// Only if pointer to it is not retained

		// Form Request Struct
		switch(*requestType)
		{
		    case ATTRQUERYRQST:

				

				attReqMsg->requestID = requestID;
				attReqMsg->numberOfQueries = DATA_TO_UDINT(decodedValues[valueOffset].data);
				valueOffset++;
				attReqMsg->names.str[0] = '\0';
				attReqMsg->names.next = NULL;
				attReqMsg->context.str[0] = '\0';
				attReqMsg->context.next = NULL;
				
				for(i = 0; i < attReqMsg->numberOfQueries;i++)
				{
					appendToLinkedList(decodedValues[valueOffset].data, decodedValues[valueOffset].len, &attReqMsg->names);
					valueOffset++;
					
					appendToLinkedList(decodedValues[valueOffset].data, decodedValues[valueOffset].len, &attReqMsg->context);
					valueOffset++;

				}
				//printf("Names: \n");
				//printLinkedList(attReqMsg->names);
				//printf("Contexts: \n");
				//printLinkedList(attReqMsg->context);
				break;
		    case SUBSCRIBE:
				subMsg->requestID = requestID;
				memcpy(subMsg->accessorPath,decodedValues[valueOffset].data, decodedValues[valueOffset].len);
				subMsg->accessorPath[decodedValues[valueOffset].len] = '\0';
					valueOffset++;
				subMsg->eventNumber = *((unsigned char*)decodedValues[valueOffset].data);
					valueOffset++;
				subMsg->cookie = *((unsigned int*)decodedValues[valueOffset].data);
					valueOffset++;
				subMsg->notifyNow = *((unsigned char*)decodedValues[valueOffset].data);
					valueOffset++;
				subMsg->notifyParam = *((unsigned int*)decodedValues[valueOffset].data);
				break;
			case SETDATAPATH:
				setdatapathMsg->requestID = requestID;
				memcpy(setdatapathMsg->accessorPath,decodedValues[valueOffset].data, decodedValues[valueOffset].len);
				setdatapathMsg->accessorPath[decodedValues[valueOffset].len] = '\0';
					valueOffset++;
				setdatapathMsg->eventNumber = *((unsigned char*)decodedValues[valueOffset].data);
					valueOffset++;
				setdatapathMsg->time = *((unsigned int*)decodedValues[valueOffset].data);
					valueOffset++;
				//setdatapathMsg->data = decodedValues[valueOffset]; // still need whole thing
				memcpy(&setdatapathMsg->data, &decodedValues[valueOffset], sizeof(IECValue)*(valueCount-valueOffset));
				break;
		    case HEARTBEAT:
		    	break;
		    default:
				printf("Unknown Message type: %02x\n", *requestType);
				break;
		}

		//printf("Part-Packet: \n");
		//printHex(packet, o, offset-o);
		return offset;
	}

	
	// Encoding functions:
	int startPacket(unsigned char* output, int offset)
	{
		output[offset+0] = 0xBE;
		output[offset+1] = 0xEE;
		output[offset+2] = 0xEE;
		output[offset+3] = 0xEF;
		return offset+4;
	}

	int endPacket(unsigned char* output, int offset)
	{
		output[offset+0] = 0xBE;
		output[offset+1] = 0xEE;
		output[offset+2] = 0xEE;
		output[offset+3] = 0xFF;
		return offset+4;
	}
	int endBadPacket(unsigned char* output, int offset)
	{
		output[offset+0] = 0xBE;
		output[offset+1] = 0xEE;
		output[offset+2] = 0xEE;
		output[offset+3] = 0xAF;
		return offset+4;
	}

	int handleAttrQueryRqst(unsigned char* output, NxtHMIAttributeQueryRequestMsg* req)
	{
		int offset = 0;
		unsigned char type = ATTRQUERYRSP;
		unsigned char valid = true;
		char* valueString = "Running";
		StringLinkedList* names = &req->names;
		StringLinkedList* contexts = &req->context;
		offset = startPacket(output, offset);
		offset = appendNxtPacketValue(output, offset, IEC_USINT, &type);

		offset = appendNxtPacketValue(output, offset, IEC_UDINT, &req->requestID);

		offset = appendNxtPacketValue(output, offset, IEC_UINT, &req->numberOfQueries);

		// Loop through each request
		// and respond with true/false present
		// 					and string of value
		while(names)
		{
			//appendNxtPacketValue(unsigned char* output, int o, IECDataType type, int len, char* value)
			if( strcmp(names->str, "Project.State") == 0 )
				valueString = ProjectState;
			else if( strcmp(names->str, "Project.Name") == 0 )
				valueString = ProjectName;
			// GUID FROM <Project>\IEC61499\DatabaseDB.xml!!!!!!!!!!!!
			else if( strcmp(names->str, "Project.Guid") == 0 )
				valueString = ProjectGuid;
			else if( strlen(names->str) > 0 )
				valueString = names->str; // dunno.. return what I got :P
			else
				valueString = "";



			offset = appendNxtPacketValue(output, offset, IEC_STRING, names->str);
			offset = appendNxtPacketValue(output, offset, IEC_STRING, contexts->str);
			offset = appendNxtPacketValue(output, offset, IEC_BOOL, &valid);
			offset = appendNxtPacketValue(output, offset, IEC_STRING, valueString);
			names = names->next;
			contexts = contexts->next;

		}
		offset = endPacket(output, offset);

		//printHex(output,0,offset);
		return offset;
	}
	
	/*
	 typedef struct
    {
	char* accessorPath;
	unsigned int eventNumber;
	unsigned int cookie;
	unsigned int notifyNow;
	unsigned int notifyParam;
    }NxtHMISubscribeMsg;
	*/
#ifdef _MSC_VER
	int handleSub(unsigned char* output, NxtHMISubscribeMsg* req, SOCKET* remotesocket)
#else
	int handleSub(unsigned char* output, NxtHMISubscribeMsg* req, int* remotesocket)
#endif
	{
		int offset = 0;
		unsigned char type = SUBSCRIBECNF;
		//unsigned char valid = true;
		char* status = "200 OK";
		int linkid = getNewLinkID();
		//IECValue* data = NULL; // temp for sending a reply here
		//int dataCount = 0;
		//int i = 0; //for looping
		subscribeSocket(req, remotesocket);
		
		//printf("Send SubCNF\n");
		offset = startPacket(output, offset);
		
		offset = appendNxtPacketValue(output, offset, IEC_USINT, &type);

		offset = appendNxtPacketValue(output, offset, IEC_UDINT, &req->requestID);

		offset = appendNxtPacketValue(output, offset, IEC_STRING, status);

		offset = appendNxtPacketValue(output, offset, IEC_UDINT, &linkid);
		offset = appendNxtPacketValue(output, offset, IEC_UINT, &req->cookie);

		offset = endPacket(output, offset);
		
		return offset;
	}

	int handleHeartBeatRsp(unsigned char* output)
	{
		int offset = 0;
		unsigned char type = HEARTBEAT;
		offset = startPacket(output, offset);
		offset = appendNxtPacketValue(output, offset, IEC_USINT, &type);
		offset = endPacket(output, offset);
		return offset;
	}


	int handleSetDataPath(unsigned char* output, NxtHMISetDataPathMsg* req)
	{
		int offset = 0;
		char* status = "200 OK";
		setData(req);
							
		offset = startPacket(output, offset);
		offset = appendNxtPacketValue(output, offset, IEC_UDINT, &req->requestID);
		offset = appendNxtPacketValue(output, offset, IEC_STRING, status);
		offset = endPacket(output, offset);

		return offset;
	}

	int sendNotification(unsigned char* output, int o, unsigned int linkid, unsigned short cookie, int dataCount, IECValuePtr* data)
	{
		int offset = o;
		int i = 0;
		unsigned char type = NOTIFY;
		time_t theTime = time(NULL);
		
		// Now send some data
		offset = startPacket(output, offset);
		offset = appendNxtPacketValue(output, offset, IEC_USINT, &type);
		offset = appendNxtPacketValue(output, offset, IEC_UDINT, &linkid);
		offset = appendNxtPacketValue(output, offset, IEC_UINT, &cookie);

		offset = appendNxtPacketValue(output, offset, IEC_DATE_AND_TIME, &theTime);
		for(i = 0; i < dataCount; i++)
		{
			
			offset = appendNxtPacketValue(output, offset, data[i].dataType, data[i].data);
			if( offset == -1 )
			{
				printf("appendNxtPacketValue failed\n");
				return -1;
			}
		}
		offset = endPacket(output, offset);
		
		return offset;
	}

	int NxtHMIServerinit()
	{
		struct sockaddr_in server;
		int flags = 0;
		//IECValue RES0Z1Interface1INITPubdata[1];
		
#ifdef _MSC_VER
		WSADATA wsaData;
		int ret = WSAStartup(0x101,&wsaData); // use highest version of winsock avaliable
		if(ret != 0)
		{
			perror("ret != 0\n");
			return -1;
		}
#endif
		// init 1st client to invalid
		#ifdef _MSC_VER
			clients[0] = INVALID_SOCKET;
		#else
			clients[0] = -1;
		#endif

		server.sin_family=AF_INET; 
		server.sin_addr.s_addr=INADDR_ANY; 
		server.sin_port=htons(HMIServerPort); 
		
		hmiserversock=socket(AF_INET,SOCK_STREAM,0); 

#ifdef _MSC_VER
		if(hmiserversock == INVALID_SOCKET)
		{
			perror("hmiserversock == INVALID_SOCKET\n");
			return -1;
		}
#else
		if(hmiserversock < 0)
		{
			perror("hmiserversock < 0 (INVALID_SOCKET)\n");
			return -1;
		}
#endif		
		if( bind(hmiserversock,(struct sockaddr*)&server,sizeof(server)) !=0 )
		{
			perror("bind(hmiserversock,(sockaddr*)&server,sizeof(server)) !=0\n");
#ifdef _MSC_VER
		printf("WSock error: %d\n", WSAGetLastError());
#endif
			return -1;
		}

		if(listen(hmiserversock,5) != 0)
		{
			perror("listen(hmiserversock,5) != 0\n");
			return -1;
		}

	// Set non-blocking read on socket
	#ifndef _MSC_VER
		if (-1 == (flags = fcntl(hmiserversock, F_GETFL, 0)))
			flags = 0;
		flags = fcntl(hmiserversock, F_SETFL, flags | O_NONBLOCK);
		if (flags != 0) {
			perror("Subl: Error Setting socket to non-blocking.");
			exit(EXIT_FAILURE);
		}
	#else
		flags = 1;
		flags = ioctlsocket(hmiserversock, FIONBIO, &flags);
		if (flags == -1) {
			perror("Subl: Error setting file status flags.");
			exit(EXIT_FAILURE);
		}
	#endif

		printf("NxtHMIServer listening on port: %d.\n", HMIServerPort);

		
		return 0; // success
	}


	void NxtHMIServerrun()
	{
		static int fromlen = sizeof(struct sockaddr_in); 
		struct sockaddr_in from;
		
		unsigned char inbuf[10240]; // big enough?
		unsigned char outbuf[10240]; // big enough?
		int iRx; // recv size
		int iTx; // transmit size
		//int valueCount = 0;
		unsigned char requestType;
		
		//int i = 0;// used for random loops
		int offset = 0, newoffset = 0;
		int client = 0;

		NxtHMIAttributeQueryRequestMsg attReqMsg;
		NxtHMISubscribeMsg subMsg;
		NxtHMISetDataPathMsg setdatapathMsg;
				

		

		//printf("NxtHMIServer run");

// Get new clients
			if( clientCount < 20 )
			{
				clients[clientCount] = accept(hmiserversock,(struct sockaddr*)&from,&fromlen);
			#ifdef _MSC_VER
				if( clients[clientCount] != INVALID_SOCKET)
			#else
				if( clients[clientCount] != -1)
			#endif
				{
					printf("Added client\n");
					clientCount++;
				}
			}

		while(client < 20)
		{
#ifdef _MSC_VER
		
			if( clients[client] == INVALID_SOCKET)
#else
			if( clients[client] == -1)
#endif
			{
				break;
			}
			
			// recv/send cmds ...
			iRx = recv(clients[client],(char*)inbuf,sizeof(inbuf),0);
			if( iRx == sizeof(inbuf) )
			{
				printf("Input buffer too small? iRx == sizeof(inbuf) [%ld]\n", sizeof(inbuf) );
			}
			if( iRx > 0 )
			{
				//printf("Got %d bytes\n", iRx);
				offset = 0;
				while(offset < iRx)
				{
					//printf("Processing Input: \n");
				
					//printf("Old Offset = %d\n", offset);
					newoffset = decodePacket(inbuf, offset, &requestType, &attReqMsg, &subMsg, &setdatapathMsg);
					if( newoffset == -1 )
					{
						printf("decodePacket Failed: hex from offset @ %d\n", offset);
						printHex(inbuf,offset,(offset+20) < iRx ? 20 : iRx-offset+5);
						// next client
						break; // end while( offset < iRx)
					}
					offset = newoffset;
					//printf("New Offset = %d\n", offset);
				
					// respond to Attribute Query Request
					switch(requestType)
					{
						case ATTRQUERYRQST:
							// form ATTRQUERYRSP
							//printf("Client %d, ATTRQUERYRQST\n", client);
							iTx = handleAttrQueryRqst(outbuf,&attReqMsg);
							break;
						case SUBSCRIBE:
							//printf("Client %d, SUBSCRIBE\n", client);
							iTx = handleSub(outbuf,&subMsg, &clients[client]);
							break;
						case SETDATAPATH: 
							//printf("Client %d, SETDATAPATH\n", client);
							iTx = handleSetDataPath(outbuf, &setdatapathMsg);
							break;
						case HEARTBEAT:
							//printf("Client %d, HEARTBEAT\n", client);
							iTx = handleHeartBeatRsp(outbuf);
							break;
						default:
							printf("Client %d, UNKNOWN\n", client);
							printf("Error: Request type %02x not handled yet.\n", requestType);
                            iTx = 0;
							break;
					}

					send(clients[client],(char*)outbuf,iTx,0);
					//printf("Sent %d bytes\n", iTx);
					//printHex(outbuf,0,iTx);
					iTx = 0;
				}
			}
			client++;
		}

		processUpdates();
	}
	
	void NxtHMIServerclose()
	{
#ifdef _MSC_VER
		closesocket(hmiserversock);
		WSACleanup(); 
		
#else
		close(hmiserversock);
#endif
	}

#ifdef __TESTING__
	int main()
	{
		NxtHMIServerinit();

		while(1)
		{
			NxtHMIServerrun();
		}


	}
#endif


#ifdef __cplusplus
}
#endif
