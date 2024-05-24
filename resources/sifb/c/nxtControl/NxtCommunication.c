#include "NxtCommunication.h"
	
	SocketLinkedList* getLastSock(SocketLinkedList *list)
	{
		SocketLinkedList* el = list;
		while(el->next != NULL )
		{
			el = el->next;
		}
		return el;
	}

	void printHex(unsigned char* buf, int offset, int len)
	{
	#ifdef DEBUG
		int i = 0;
		for(i = 0; i < 6;i++)
			printf("   ");

		for(i = 0; i < len; i++)
		{
			printf(" %02X", buf[offset+i] & 0xff); // dammit 1 just want 2 characters from my 1 byte
			//if( (i+1) % 2 == 0) printf(" ");
			if( (i+6+1) % 16 == 0) printf("\n");
		}
		printf("\n");
	#endif
	}

	unsigned int fixInputData(unsigned char* packet, int offset, int datalen)
	{
		int i,j;
		unsigned int escapeCount = 0;
		for(i = 0; i < datalen; i++)
		{
			if( packet[offset+i] == 0xBE )
			{
				if( packet[offset+i+1] == 0x00 )// next byte is 0x00 'escape'
				{
					escapeCount++;
					// shift remaining data down one
					for(j = i+1; j < datalen; j++)
					{
						packet[offset+j] = packet[offset+j+1];
					}
				}
			}
		}
		return escapeCount;
	}


	int decodeNxtPacketValue(unsigned char* packet, int o, IECValue* decodedValue)
	{
		int offset = o;
		int datalen = 1;

		bool reverseData = true;
		unsigned short valueType = packet[offset];
		unsigned int escapeCount = 0; // for skipping extra bytes if there are escape chars
		int i = 0;
		//printf("valueType = %02x\n", valueType);
			
		// If valueType does not have IEC_PRIMITIVE & IEC_APPLICATION bits set.. its not what we are looking for
		if ( (valueType & (IEC_PRIMITIVE | IEC_APPLICATION)) !=
																(IEC_PRIMITIVE | IEC_APPLICATION) )
		{
			//printf("'%02X' Invalid ASN1 tag\n", valueType & 0xff);
			return -1;
		}
		valueType &= ~(IEC_PRIMITIVE | IEC_APPLICATION); // get rid of those bits
		//printf("ValueType = %02x", valueType);
		offset += 1; // after type thing
		
		decodedValue->dataType = valueType;
			
		datalen = 1; // just a var for setting data for bool
		switch (valueType)
		{
			case IEC_ANY: // Actually BOOL - false
				datalen = 0; // just a var for setting data
			case IEC_BOOL: // Actually BOOL - true
				memcpy(decodedValue->data, &datalen, 1);
				decodedValue->len = datalen;
				return offset;
			case IEC_SINT:
			case IEC_USINT:
			case IEC_BYTE:
				datalen = 1;
				break;
			case IEC_INT:
			case IEC_UINT:
			case IEC_WORD:
				datalen = 2;
				break;
			case IEC_DINT:
			case IEC_UDINT:
			case IEC_DWORD:
			case IEC_REAL:
				datalen = 4;
				break;
			case IEC_LINT:
			case IEC_ULINT:
			case IEC_LWORD:
			case IEC_LREAL:
			case IEC_TIME:
			case IEC_DATE:
			case IEC_TIME_OF_DAY:
			case IEC_DATE_AND_TIME:
				datalen = 8;
				break;
			case IEC_STRING:
			case IEC_WSTRING:
				// Varies
				datalen = (packet[offset] << 8) | (packet[offset + 1]);
				//printf("Str len = %d\n", datalen);
				offset+=2;
				reverseData = false;
				break;
			default:
				printf("\nUnexpected value type %02x\n", valueType);
				return -1;
		}
		
		// Check data for escape things
		// eg 0xBE MUST be followed by 0x00
		escapeCount = fixInputData(packet,offset,datalen); // change packet so reverse and memcpy are still nice

		memset(decodedValue->data, 0, sizeof(decodedValue->data));

		if( reverseData )
		{
		    for(i = datalen -1; i >=0; i--)
		    {
			   //printf("i = %d, data[%d], packet[%d] = %02x. ", i, datalen-1-i, offset+i, packet[offset+i]);
			   memcpy(&(decodedValue->data)[datalen-1-i], &(packet[offset+i]), sizeof(char));
			  

			   //printf("\tdata[%d] = %02x\n", datalen-1-i, (*decodedValue).data[datalen-1-i]);
		    }
		}
		else
		{
		    memcpy(decodedValue->data, &(packet[offset]), sizeof(char)*datalen);
		}
		
		
		//printf("\tCopy %d bytes to %p\n", sizeof(char)*datalen, (*decodedValue)->data, decodedValue);
		offset += datalen; // increment offset (data 'consumed')
		offset += escapeCount; // Skip # of escape bytes
		decodedValue->len = datalen;
		
		return offset;
	}
	
	
	// len ignored unless type ~~ STRING
	/*
	 * TODO:
	 * Send the data. If the data octet to be sent is 0xBE, inspect following
		bytes
		? if the next octet is known and is 0x00, stuff the 0x00
		? if at least three octets are known
			? if they match a marker, stuff a 0x00 after the 0xBE
			? if they do not, just send them
		? if the encoder does not know the next octets yet, just stuff the 0x00
			after the 0xBE

			 *
	 */
	int appendNxtPacketValue(unsigned char* output, int o, IECDataType type, void* valptr)
	{
		char* value = (char*)valptr;

		int offset = o;
		unsigned short datalen = 1;

		int i = 0;
		
		bool reverseData = true;
		unsigned char valueType = type | (IEC_PRIMITIVE | IEC_APPLICATION);

		if( type == IEC_BOOL ) // special
		{
			if( *value == 0 ) // TODO: check.. The 'type' code essentially becomes 0. How do we tell if IEC_ANY or IEC_BOOL?
				output[offset] = (IEC_PRIMITIVE | IEC_APPLICATION);
			else
				output[offset] = valueType;
			offset++;
			return offset;
		}

		output[offset] = valueType;
		offset += 1; // after type thing

		switch (type)
		{
			//case IEC_BOOL:
			case IEC_SINT:
			case IEC_BYTE:
			case IEC_USINT:
				datalen = 1;
				break;
			case IEC_INT:
			case IEC_UINT:
			case IEC_WORD:
				datalen = 2;
				break;
			case IEC_DINT:
			case IEC_UDINT:
			case IEC_DWORD:
			case IEC_REAL:
				datalen = 4;
				break;
			case IEC_LINT:
			case IEC_ULINT:
			case IEC_LWORD:
			case IEC_LREAL:
			case IEC_TIME:
			case IEC_DATE:
			case IEC_TIME_OF_DAY:
			case IEC_DATE_AND_TIME:
				datalen = 8;
				break;
			case IEC_STRING:
			case IEC_WSTRING:
				// Varies
				datalen = strlen(value); // does NOT include '\0'
				// Strings need datalen written to output
				// REVERSE THE ORDER
				output[offset+0] = ((char*)&datalen)[1];
				output[offset+1] =  ((char*)&datalen)[0];
				offset += 2;
				reverseData = false;
				break;
			default:
				printf("\nUnexpected value type %02x\n", type & 0xff);
				
				return -1;
		}

		if( reverseData )
		{
			for(i = datalen -1; i >=0; i--)
			{
			   //printf("i = %d, output[%d], value[%d] = %02x. \n", i,  offset+i, datalen-1-i, (value)[datalen-1-i] & 0xff);
			   memcpy(&output[offset+i], &value[datalen-1-i], sizeof(char));
			}
		}
		else
		{
			memcpy(&output[offset], value, sizeof(char)*datalen);
		}


		//printf("\tCopy %d bytes to output\n", sizeof(char)*datalen);
		offset += datalen; // increment offset (data 'consumed')
		return offset;
	}
