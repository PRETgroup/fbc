#ifdef __cplusplus
extern "C"{
#endif

#include <stdlib.h>
#include <stdio.h>

#include "NxtHMIData.h"


		void appendToLinkedList(char* str, int len, StringLinkedList* listNode)
		{
			StringLinkedList* node = listNode;
			// Traverse LinkedList to find empty str
			while(1)
			{
				if( !node->inuse ) //strlen(node->str) == 0 )
				{
					memcpy(node->str,str,sizeof(char) * len);
					node->str[len] = '\0'; // end string properly (0-(len-1) = string)
					node->inuse = true;
					break;
				}
				else if (node->next == NULL)
				{
					node->next = (StringLinkedList*)malloc(sizeof(StringLinkedList));

					node->next->str[0] = '\0';
					node->next->inuse = false;
					node->next->next = NULL;
				}
				//else
				node = node->next;
			}
		}
	
		void printLinkedList(StringLinkedList listNode)
		{
			StringLinkedList* node = &listNode;
			while(1)
			{
				if( node->str != NULL )
				{
					printf("String: %s\n", node->str);
				}
				if (node->next == NULL)
				{
					break;
				}
				//else
				node = node->next;
			}
		}

		/*void newIECValue(IECValue** data)
		{
			*data = (IECValue*)malloc(sizeof(IECValue));
			(*data)->data = NULL;
		}

		void freeIECValue(IECValue* data, bool andData)
		{
			if(andData && data->data != NULL)
			{
				free(data->data);
			}

			free(data);
		}*/

		/*void emptyStringLinkedList(StringLinkedList* list)
		{
			// Traverse LinkedList to find empty str
			if(list != NULL)
			{
				if( list->str != NULL )
				{
					free(list->str);
				}
				if (list->next != NULL)
				{
					emptyStringLinkedList(list->next);
					free(list->next);
				}
			} // list not freed!
		}*/

			/*	typedef struct
    {
		unsigned int requestID;
		unsigned short numberOfQueries;
		StringLinkedList names;
		StringLinkedList context;
    }NxtHMIAttributeQueryRequestMsg;*/
		/*void freeNxtHMIAttributeQueryRequestMsg(NxtHMIAttributeQueryRequestMsg* decodedPacket)
		{
			emptyStringLinkedList(&decodedPacket->names);
			emptyStringLinkedList(&decodedPacket->context);
			free(decodedPacket);
		}*/

			/*enum NotifyNow {
		NOT_IMMEDIATE = 0,
		SEND_CURRENT = 1,
		GET_HISTORICAL_LIST = 2, // notfyParam = # of history
		GET_PAST_HISTORY = 3, // notfyParam = timestamp to start from
	};

	
    typedef struct
    {
		unsigned int requestID;
		char* accessorPath;
		unsigned char eventNumber;	//bit 0-4 event# to sub to, starting with ev#0
									//bit 7 = 0 (input event (runtime->Canvas))
									//bit 7 = 1 (output event (Canvas->runtime))
		unsigned short cookie;		// arbitrary 16bit #
		unsigned char notifyNow;		//
		unsigned int notifyParam;	// param for notifyNow option 2/3
    }NxtHMISubscribeMsg;*/
		/*void freeNxtHMISubscribeMsg(NxtHMISubscribeMsg* decodedPacket)
		{
			free(decodedPacket->accessorPath);
			free(decodedPacket);
		}*/

		/*typedef struct
    {
		unsigned int requestID;
		char* accessorPath;
		unsigned char eventNumber;	//bit 0-4 event#, starting with ev#0
									//bit 7 ALWAYS = 1 (output event (Canvas->runtime))
		time_t time; // TO Be Done by next control... send UDINT 0 for now
		IECValue* data;
    }NxtHMISetDataPathMsg;*/
		/*void freeNxtHMISetDataPathMsg(NxtHMISetDataPathMsg* decodedPacket, int dataCount)
		{
			int d = 0;
			free(decodedPacket->accessorPath);
			for(d = dataCount-1; d>=0; d--)
			{
				free(&decodedPacket->data[d]);
			}
			free(decodedPacket);
		}*/
		
#ifdef __cplusplus
}
#endif
