// This file is generated by FBC.
// COMMCHANNEL stuff done in COMMCHANNEL.. this is just a place holder
#include "COMMCHANNELUDPRECV.h"
#include <stdio.h>
#include <string.h>

/* COMMCHANNELUDPRECV initialization function */
void COMMCHANNELUDPRECVinit(COMMCHANNELUDPRECV* me)
{
    memset(me, 0, sizeof(COMMCHANNELUDPRECV));
}
	
int COMMCHANNELUDPRECVopen(COMMCHANNELUDPRECV* me, char* ownAddr)
{
		char *token;
		int returnValue;
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

		me->server.sin_family=AF_INET;
		// Get own addr details
		token = strtok(ownAddr, ":");
		// ignore IP
		//server.sin_addr.s_addr = inet_addr(token);
		me->server.sin_addr.s_addr = INADDR_ANY;
		token = ownAddr + strlen(token) + 1;
		me->server.sin_port = htons( (unsigned short)strtol(token, NULL, 10) );
		
		me->channelRecvSock=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
		//me->channelRecvSock=socket(AF_INET,SOCK_STREAM,0); 

#ifdef _MSC_VER
		if(me->channelRecvSock == INVALID_SOCKET)
		{
			perror("me->channelRecvSock == INVALID_SOCKET\n");
			return -1;
		}
#else
		if(me->channelRecvSock < 0)
		{
			perror("me->channelRecvSock < 0 (INVALID_SOCKET)\n");
			return -1;
		}
#endif		
		if( bind(me->channelRecvSock,(struct sockaddr*)&me->server,sizeof(me->server)) !=0 )
		{
			perror("bind(me->channelRecvSock,(sockaddr*)&me->server,sizeof(me->server)) !=0\n");
#ifdef _MSC_VER
		printf("WSock error: %d\n", WSAGetLastError());
#endif
			return -1;
		}

		/*if((returnValue = listen(me->channelRecvSock,5)) == SOCKET_ERROR)
		{
			printf("listen(me->channelRecvSock,5) == SOCKET_ERROR %d\n", returnValue);
			#ifdef _MSC_VER
					printf("WSock error: %d\n", WSAGetLastError());
			#endif
			return -1;
		}*/

		setNonblocking(me->channelRecvSock);

		printf("NxtCOMMCHANNELUDPRECV listening on port: %s.\n", token);

		
		return 0; // success
	}

/* COMMCHANNELUDPRECV execution function */
void COMMCHANNELUDPRECVrun(COMMCHANNELUDPRECV* me)
{
	me->_output.events = 0;

		me->_output.events = 0;

	if( me->inited )
	{
		CommChannelClearEvents();
		COMMCHANNELUDPRECVread(me);
	}
	else if( me->_input.event.INIT) {
		
		COMMCHANNELUDPRECVopen(me, me->_OWNADDR);
		
		me->_output.event.INITO = 1;
		
		me->inited = true;
	}
		
	me->_input.events = 0;
		
	me->_input.events = 0;
	
}


void COMMCHANNELUDPRECVread(COMMCHANNELUDPRECV* me)
{
	static int fromlen = sizeof(struct sockaddr_in); 
	struct sockaddr_in from;

	unsigned char inbuf[1024]; // big enough?
	int iRx; // recv size
	int valueCount = 0;
	unsigned char requestType;
		
	int i = 0;// used for random loops
	int offset = 0;
	
	while(1)
	{
		// recv/send cmds ...
		memset(inbuf, 0 , 1024);
		iRx = recv(me->channelRecvSock,(char*)inbuf,sizeof(inbuf),0);
		if( iRx == sizeof(inbuf) )
		{
			printf("Input buffer too small? iRx == sizeof(inbuf) [%ld]\n", sizeof(inbuf) );
			// skip assuming the packet is corrupted
			break;
		}
		else
		{
			if( iRx > 0 )
			{
				//printf("Got %d bytes\n", iRx);
						
				offset = 0;
				while(offset < iRx)
				{
					//printf("Processing Input: \n");
					
					//printf("Old Offset = %d\n", offset);
					offset = CommChannelHandlePacket(inbuf, iRx, offset);
					if( offset == -1 )
					{
						printf("CommChannelHandlePacket Failed\n");
						printHex(inbuf, 0, iRx);
						// next client
						break;
					}

				}
			}
			else
				break;
		}
	}
		
}


int CommChannelHandlePacket(unsigned char *inbuf, int length, int o)
{
	int offset = o;
	IECValue tempV, sourceV, chanidV;
	
	unsigned char command;
	char source[64];
	unsigned long long chanid;

	
	
	//decode using int decodeNxtPacketValue(unsigned char* packet, int o, IECValue* decodedValue);

	// Version
	offset = decodeNxtPacketValue(inbuf, offset, &tempV);
	if( tempV.dataType != IEC_USINT )
	{
		
		//printHex(inbuf,0,o+10);
		return -1;
	}
	if( offset == -1 )
		return -1;

	// Source
	offset = decodeNxtPacketValue(inbuf, offset, &sourceV);
	if( offset == -1 )
		return -1;
	strcpy(source, sourceV.data);
	// Command
	offset = decodeNxtPacketValue(inbuf, offset, &tempV);
	if( offset == -1 )
		return -1;
	command = tempV.data[0];
	// ChanID
	offset = decodeNxtPacketValue(inbuf, offset, &chanidV);
	if( offset == -1 )
		return -1;
	memcpy(&chanid, chanidV.data, 8);

	switch( command )
	{
		case 0x01:
			// TODO: if command = keep alive, update 'last heard from'
			break;
		case 0x82:
		#ifdef DEBUG
			printf("From: %s, got FB Command: %02x for Chan %lld\n", source, command & 0xff, chanid);
		#endif	
			offset = CommChannelHandleFBData(chanid, source, inbuf, length, offset);
			break;
		case 0x87:
		#ifdef DEBUG
			printf("From: %s, got ADP Command: %02x for Chan %lld\n", source, command & 0xff, chanid);
		#endif	
			offset = CommChannelHandleAdapterData(chanid, source, inbuf, length, offset);
			break;
	}
	return offset;
}


