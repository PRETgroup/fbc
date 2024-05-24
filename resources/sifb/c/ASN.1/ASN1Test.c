#include <stdio.h>
#include "ASN1.h"

void printHex(unsigned char* buf, int offset, int len)
{
	int i = 0;
	
	for(i = 0; i < len; i++)
	{
		printf(" %02X", buf[offset+i] & 0xff); // dammit I just want 2 characters from my 1 byte
		//if( (i+1) % 2 == 0) printf(" ");
		if( (i+1) % 16 == 0) printf("\n");
	}
	printf("\n");
}
	
int encodeTest(unsigned char* output)
{
	int offset = 0, newoffset;
	char bValue;
	short sValue;
	int dValue;
	int dAValue[3];
	unsigned int udValue;
	long lValue;
	printf("Encoding: BOOL FALSE:\n");
	bValue = 0;
	newoffset = encodeDERValue(output, offset, IEC_BOOL, 1, &bValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: BOOL TRUE:\n");
	bValue = 1;
	newoffset = encodeDERValue(output, offset, IEC_BOOL, 1, &bValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: SINT 55:\n");
	bValue = 55;
	newoffset = encodeDERValue(output, offset, IEC_SINT, 1, &bValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: USINT 98:\n");
	bValue = 98;
	newoffset = encodeDERValue(output, offset, IEC_USINT, 1, &bValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: USINT 130:\n");
	bValue = 130;
	newoffset = encodeDERValue(output, offset, IEC_USINT, 1, &bValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: UINT 55555:\n");
	sValue = 55555;
	newoffset = encodeDERValue(output, offset, IEC_UINT, 1, &sValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: UDINT 55555:\n");
	udValue = 55555;
	newoffset = encodeDERValue(output, offset, IEC_UDINT, 1, &udValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	printf("Encoding: DINT[3] {33333,44444,55555}:\n");
	dAValue[0] = 33333;
	dAValue[1] = 44444;
	dAValue[2] = 55555;
	newoffset = encodeDERValue(output, offset, IEC_DINT, 3, &dAValue);
	printHex(output, offset, newoffset-offset);
	offset = newoffset;
	
	return offset;
}

int decodeTest(unsigned char* input)
{
	int offset = 0, newoffset;
	char bValue;
	unsigned char ubValue;
	short sValue;
	unsigned short usValue;
	int dValue;
	int dAValue[3];
	unsigned int udValue;
	long lValue;
	
	newoffset = decodeDERValue(input, offset, &bValue, sizeof(bValue));
	printf("Decoding: BOOL FALSE:\n");
	printHex(&bValue, 0, sizeof(bValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &bValue, sizeof(bValue));
	printf("Decoding: BOOL TRUE:\n");
	printHex(&bValue, 0, sizeof(bValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &bValue, sizeof(bValue));
	printf("Decoding: SINT 55 = %d\n", bValue);
	printHex(&bValue, 0, sizeof(bValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &ubValue, sizeof(ubValue));
	printf("Decoding: USINT 98 = %d\n",ubValue);
	printHex(&ubValue, 0, sizeof(ubValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &ubValue, sizeof(ubValue));
	printf("Decoding: USINT 130 = %d\n", ubValue);
	printHex(&ubValue, 0, sizeof(ubValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &usValue, sizeof(usValue));
	printf("Decoding: UINT 55555 = %d\n", usValue);
	printHex(&usValue, 0, sizeof(usValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &udValue, sizeof(udValue));
	printf("Decoding: UDINT 55555 = %d\n", udValue);
	printHex(&udValue, 0, sizeof(udValue));
	offset = newoffset;
	
	newoffset = decodeDERValue(input, offset, &dAValue, sizeof(dAValue));
	printf("Decoding: DINT[3] = {%d,%d,%d}\n", dAValue[0], dAValue[1], dAValue[2]);
	printHex(&dAValue, 0, sizeof(dAValue));
	offset = newoffset;
	
	return offset;
}
	
int main(int argc, char* argv[])
{

	unsigned char output[2048];
	int encOffset, decOffset;
	encOffset = encodeTest(output);
	printf("Encoded Packet:\n");
	printHex(output, 0, encOffset);
	
	decOffset = decodeTest(output);
	if( encOffset != decOffset )
		printf("CODEC Failed.\n");
	else
		printf("CODEC Successful (probably).\n");
	return 0;
}
