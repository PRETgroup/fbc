#include <stdio.h>
#include "ASN1.h"
#include "PUBLISH_8.h"
#include "SUBSCRIBE_8.h"
#include <string.h>

void printHex(unsigned char* buf, int offset, int len)
{
#ifdef DEBUG
	int i = 0;
	for(i = 0; i < len; i++)
	{

		printf(" %02X", buf[offset+i] & 0xff); // dammit I just want 2 characters from my 1 byte
		if( (i+1) % 16 == 0) printf("\n");
	}
	printf("\n");
#endif
}

	
int main(int argc, char* argv[])
{
#define LOOPLIMIT 50
	int loop = 0;
	PUBLISH_8 puber;
	SUBSCRIBE_8 suber;
	
	PUBLISH_8init(&puber);
	SUBSCRIBE_8init(&suber);
		
	puber._QI = true;
	strcpy(puber._ID, "224.0.0.1:61499");
	suber._QI = true;
	strcpy(suber._ID, "224.0.0.1:61499");
	
	PUBLISH_8run(&puber);
	SUBSCRIBE_8run(&suber);
	puber._input.event.INIT = 1;
	suber._input.event.INIT = 1;
	PUBLISH_8run(&puber);
	SUBSCRIBE_8run(&suber);
		
	
	for(;loop < LOOPLIMIT; loop++)
	{
		printf("%d: ", loop);
		puber._input.event.REQ = 1;
		puber._SD_1 = ~puber._SD_1;
		puber._SD_2 = ~puber._SD_1;
		puber._SD_3 = loop;
		puber._SD_4 = loop;
		puber._SD_5 = ~loop;
		puber._SD_6 = loop*64;
		sprintf(puber._SD_7, "Loop: %d", loop);
		puber._SD_8[0] = loop;
		puber._SD_8[1] = loop*2;
		puber._SD_8[2] = loop*4;

		PUBLISH_8run(&puber);
		SUBSCRIBE_8run(&suber);

		fflush(stdout);
		// Check
		if( puber._SD_1 != suber._RD_1 )
			break;
		if( puber._SD_2 != suber._RD_2 )
			break;
		if( puber._SD_3 != suber._RD_3 )
			break;
		if( puber._SD_4 != suber._RD_4 )
			break;
		if( puber._SD_5 != suber._RD_5 )
			break;
		if( puber._SD_6 != suber._RD_6 )
			break;
		//if( puber._SD_7 != suber._RD_7 )
		if( strcmp(puber._SD_7, suber._RD_7) != 0 )
			break;
		if( puber._SD_8[0] != suber._RD_8[0] )
			break;
	}
	if( loop < (LOOPLIMIT-1) )
		printf("Codec Error\n");
	return 0;
}
