#include "datatypes.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h> // strtol etc

#if _MSC_VER
#define strcasecmp stricmp
#endif

/***************************************************************
	toString
***************************************************************/
// Bit strings
int BOOLtoString(char* str, BOOL* value)
{
	if( *value )
		strcpy(str, "TRUE");
	else
		strcpy(str, "FALSE");
	return strlen(str);
}

int BYTEtoString(char* str, BYTE* value)
{
	sprintf(str, "%02X", *value);
	return strlen(str);
}

int WORDtoString(char* str, WORD* value)
{
	sprintf(str, "%04X", *value);
	return strlen(str);
}

int DWORDtoString(char* str, DWORD* value)
{
	sprintf(str, "%08X", *value);
	return strlen(str);
}

int LWORDtoString(char* str, LWORD* value)
{
	sprintf(str, "%016llX", *value);
	return strlen(str);
}

// Integers
int SINTtoString(char* str, SINT* value)
{
	sprintf(str, "%d", *value);
	return strlen(str);
}

int USINTtoString(char* str, USINT* value)
{
	sprintf(str, "%u", *value);
	return strlen(str);
}

int INTtoString(char* str, INT* value)
{
	sprintf(str, "%d", *value);
	return strlen(str);
}

int UINTtoString(char* str, UINT* value)
{
	sprintf(str, "%u", *value);
	return strlen(str);
}

int DINTtoString(char* str, DINT* value)
{
	sprintf(str, "%d", *value);
	return strlen(str);
}

int UDINTtoString(char* str, UDINT* value)
{
	sprintf(str, "%u", *value);
	return strlen(str);
}

int LINTtoString(char* str, LINT* value)
{
	sprintf(str, "%lld", *value);
	return strlen(str);
}

int ULINTtoString(char* str, ULINT* value)
{
	sprintf(str, "%llu", *value);
	return strlen(str);
}

// Reals
int REALtoString(char* str, REAL* value)
{
	sprintf(str, "%f", *value);
	return strlen(str);
}

int LREALtoString(char* str, LREAL* value)
{
	sprintf(str, "%f", *value);
	return strlen(str);
}


/***************************************************************
	valueOf return success / fail
***************************************************************/
// Bit strings
bool valueOfBOOL(BOOL* value, char* str)
{
	if( strcasecmp(str, "TRUE") == 0 )
	{
		*value = TRUE;
		return TRUE;
	}
	else if( strcasecmp(str, "FALSE") == 0 )
	{
		*value = FALSE;
		return TRUE;
	}
	else if( strcasecmp(str, "1") == 0 )
	{
		*value = TRUE;
		return TRUE;
	}
	else if( strcasecmp(str, "0") == 0 )
	{
		*value = FALSE;
		return TRUE;
	}
	return FALSE; // parse failed
}

bool valueOfBYTE(BYTE* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfWORD(WORD* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfDWORD(DWORD* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfLWORD(LWORD* value, char* str)
{
	return FALSE; // parse failed
}

// Integers
bool valueOfSINT(SINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfUSINT(USINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfINT(INT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfUINT(UINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfDINT(DINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfUDINT(UDINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfLINT(LINT* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfULINT(ULINT* value, char* str)
{
	return FALSE; // parse failed
}

// Reals
bool valueOfREAL(REAL* value, char* str)
{
	return FALSE; // parse failed
}

bool valueOfLREAL(LREAL* value, char* str)
{
	return FALSE; // parse failed
}

// TODO: Date/Time types

// TODO: Bother with STRING? maybe with WSTRING
