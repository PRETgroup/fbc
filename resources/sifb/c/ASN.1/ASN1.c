#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "ASN1.h"

	void copyReverseBytes(unsigned char *out, int offsetOut, unsigned char *in, unsigned int datalen)
	{
		int i;
		for(i = datalen -1; i >=0; i--)
		{
			memcpy(&(out)[offsetOut+i], &(in[datalen-1-i]), sizeof(char));
#ifdef DEBUG
			printf("IN: %02x, now OUT: %01x\n", (in[datalen-1-i]), (out)[offsetOut+i]);
#endif
		}
	}

	// TODO: SEQUENCE DECODING currently lazy and potential to cause SEGFAULT (&value+('ASN.1 datalen of last datatype') assumed to be pointer to next value)
	int decodeDERValue(unsigned char* packet, int o, void* value, unsigned int valueSize)
	{
		int offset = o;
		int seqEndOffset, valueOffset = 0, newOffset; // all added for SEQUENCEs
		int remainingSize = 0;
		unsigned char valueType = packet[offset++];
		unsigned char tempByte = 0;
		ASN1DataType dataType;
		unsigned int datalen = 0;
		int i = 0;
		
		// TODO: UDTs not handled
		// TODO: How do I know based on bits 7 & 8 what I can decode...
		// Currently only UNIVERSAL PRIMITIVEs
		if ( (valueType & (ASN1_APPLICATION)) == (ASN1_APPLICATION) )
		{
			printf("'%02X' UN-HANDLED ASN.1 tag\n", valueType & 0xff);
			return -1;
		}
		
		// TODO: Assumes Primitive:
		valueType &= ~(ASN1_PRIMITIVE | ASN1_APPLICATION); // get rid of those bits
		//printf("ValueType = %02x", valueType);
		dataType = valueType;
		
// Read Data length (in DER all lengths are DEFINATE (known vs uses a marker to end))		
		if( (packet[offset] & 0x80) == 0x80 ) // Long form of Length
		{
			tempByte = packet[offset++] & 0x7F; // lower 7 bits = length of length
			if( tempByte > 4 ) // > 4 BYTES worth of LENGTH... X.690-0207.pdf says unsigned binary integer
			{
				printf("decodeDERValue with length of length > 4 bytes not handled. (%d bytes)\n Sorry...", tempByte);
				exit(-1);
			}
			for(i=0; i<tempByte; i++)
			{
				datalen += packet[offset+i];
				datalen = datalen << 8;
			}
			
			
			offset += tempByte;			
		}
		else // short form of length
		{
			datalen = packet[offset++]; 
		}
		
		memset(value, 0, valueSize); //clear data
		
		if( valueSize < datalen )
		{
				printf("WARNING: Dest buffer < incoming datalen.. MSB's of data will be truncated.\n");
		}
		switch (valueType)
		{
			case ASN1_BOOLEAN:
				if( packet[offset] == 0 ) 
					((char*)value)[0] = 0;
				else // NOTE: 0xFF should be sent for TRUE... but meh
					((char*)value)[0] = 0xFF;
				break;
			case ASN1_INTEGER:
				// TODO: What about the extra padding on Unsigned positive #s?
				copyReverseBytes(value, 0, &packet[offset], datalen);
				break;
			case ASN1_BITSTRING:
				break;
			case ASN1_OCTETSTRING:
				break;
			case ASN1_NULL:
				// Length should always be 0
				// Empty Value
				break;
			case ASN1_OBJECTIDENTIFIER:
				break;
			case ASN1_OBJECTDESCRIPTOR:
				break;
			case ASN1_EXTERNAL:
				break;
			case ASN1_REAL:
				break;
			case ASN1_ENUM:
				break;
			case ASN1_EMBEDDED_PDV:
				break;
			case ASN1_GENERALSTRING:
				if( datalen < valueSize )
					memcpy(value, &packet[offset], datalen);
				else
					memcpy(value, &packet[offset], valueSize);
				break;
			case ASN1_RELATIVEOBJECTID:
				break;
			case ASN1_SEQUENCE: // array / UDT
				seqEndOffset = offset + datalen;
				remainingSize = valueSize; // stores max remaining size for value
				while( offset < seqEndOffset  ) // get everything inside
				{
	#ifdef DEBUG
					printf("Getting Sequence value.\n");
	#endif
					newOffset = decodeDERValue(packet, offset, value+valueOffset, remainingSize);

					// TODO: next two lines SHOULD DO 61499 Datalen!!!
					valueOffset += (newOffset-offset-2); // -2 for type + length.. so just increment by **ASN1** datalen
					remainingSize -= (newOffset-offset);

					offset = newOffset;
				}
				offset = seqEndOffset - datalen; // reset offset :(
				break;
			default:
				printf("\nUnexpected value type %02x\n", valueType);
				return -1;
		}
		
		offset += datalen; // increment offset (data 'consumed')
#ifdef DEBUG
		printf("Decoded %d bytes.\n", offset - o);
#endif
		return offset;
	}
	
	int encodeDERValue(unsigned char* output, int o, IECDataType type, int arraysize, void* valptr)
	{
		char* value = (char*)valptr;

		int offset = o;
		int arrayindex = 0;
		unsigned short datalen = 1;

		if( arraysize > 1 )
		{
			// Define SEQUENCE
			output[offset++] = ASN1_SEQUENCE | (ASN1_UNIVERSAL);
			output[offset++] = (1+1+IECLength[type])*arraysize; // length of sequence == (1+1+IECLength[type])*arraysize
		}
		
		for(; arrayindex < arraysize; arrayindex++)
		{
			switch (type)
			{
				case IEC_BOOL:
					output[offset++] = ASN1_BOOLEAN | (ASN1_UNIVERSAL);
					// Len = 1
					output[offset++] = 1;
					if( *value == 0 )
						output[offset] = 0;
					else
						output[offset] = 0xFF; // BOOLEAN TRUE == 0xFF
					offset += IECLength[type];
					break;
				// Signed Integers (will already be in 2s Complement format) 
				// TODO: Are there platforms that don't store -#s in 2s complement?
				case IEC_SINT:
				case IEC_INT:
				case IEC_DINT:
				case IEC_LINT:
					output[offset++] = ASN1_INTEGER | (ASN1_UNIVERSAL);
					output[offset++] = IECLength[type];
					copyReverseBytes(output, offset, value+(IECLength[type]*arrayindex), IECLength[type]);
					offset += IECLength[type];
					break;
					
				case IEC_USINT:
				case IEC_UINT:
				case IEC_UDINT:
				case IEC_ULINT:
					output[offset++] = ASN1_INTEGER | (ASN1_UNIVERSAL);

					if( (value[IECLength[type]-1] & 0x80) == 0x80 )
					{
					// if the highest bit is set (and this is UNSIGNED i.e. +tve) .. add extra length and set 1st content to all 0
	#ifdef DEBUG
					printf("MSB = %02x. Adding extra length and empty content for positive number.\n", value[IECLength[type]-1] & 0xFF);
	#endif
						output[offset++] = IECLength[type] + 1;
						output[offset++] = 0x00;
					}
					else
					{
						output[offset++] = IECLength[type];
					}
					copyReverseBytes(output, offset, value+(IECLength[type]*arrayindex), IECLength[type]);
					offset += IECLength[type];
					break;
				case IEC_STRING:
				case IEC_WSTRING:
					output[offset++] = ASN1_GENERALSTRING | (ASN1_UNIVERSAL); // GeneralString allows '\n' etc
					datalen = strlen(value);
					if( datalen > 127 ) // longer form (TODO: Current assumed 2 bytes len MAX)
					{
						output[offset++] = 0x80 & ((char*)&datalen)[0];
						output[offset++] = ((char*)&datalen)[1];
					}
					else
					{
						output[offset++] = datalen;
					}
					strcpy(&output[offset], value);
					offset += datalen;
					break;
				default:
					printf("\nUnexpected value type %02x\n", type);
					return -1;
			}
		}

#ifdef DEBUG
		printf("Encoded %d bytes.\n", offset - o); 
#endif
		
		return offset;
	}
