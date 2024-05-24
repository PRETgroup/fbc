#include "Simulation.h"


// This stuff is just here for milestone 3
// need to add this code when testing on windows
/*#ifdef _MSC_VER
#include <windows.h>
#include <time.h>
int gettimeofday(struct timeval* tp, void* tzp)
{
	DWORD t;
	t = GetTickCount();
	tp->tv_sec = t / 1000;
	tp->tv_usec = (t % 1000) * 1000;
	return 0; // 0 indicates that the call succeeded.
}
void timersub( const struct timeval * tvp, const struct timeval * uvp, struct timeval
* vvp )
{
	vvp->tv_sec = tvp->tv_sec - uvp->tv_sec;
	vvp->tv_usec = tvp->tv_usec - uvp->tv_usec;
	if( vvp->tv_usec < 0 )
	{
	   --vvp->tv_sec;
	   vvp->tv_usec += 1000000;
	}
}
#endif*/

void voidToString(void* value, IEC61131Type type, int arraylen, char* result)
{
	int i;
    unsigned int ui;
	int offset = 0; // offset in output string
	int index;
	if( arraylen > 1 )
	{
		result[0] = '[';
		offset = 1;
	}

	for(index = 0; index < arraylen; index++)
	{
		switch(type)
		{
			case IEC_INT:
				i = ((short*)value)[index];
				sprintf(result + offset,"%d",i);
				break;
			case IEC_BYTE:
			case IEC_CHAR:
				i = ((unsigned char*)value)[index];
				sprintf(result + offset,"%d",i);
				break;
			case IEC_UINT:
				ui = ((unsigned short*)value)[index];
                sprintf(result + offset,"%d",ui);
				break;
			case IEC_DINT:
                sprintf(result + offset,"%d",((int*)value)[index]);
				break;
			case IEC_UDINT:
				sprintf(result + offset,"%d",((unsigned int*)value)[index]);
				break;
			case IEC_LINT:
				sprintf(result + offset,"%ld",((long*)value)[index]);
				break;
			case IEC_ULINT:
				sprintf(result + offset,"%ld",((unsigned long*)value)[index]);
				break;
			case IEC_REAL:
				sprintf(result + offset, "%f", ((float*)value)[index]);
				break;
			case IEC_LREAL:
				sprintf(result + offset, "%f", ((double*)value)[index]);
				break;
			case IEC_BOOL:
				i = ((bool*)value)[index];
                sprintf(result + offset,"%d",i); // should just be 0 or 1
				break;
			case IEC_STRING:
			case IEC_WSTRING:
				result[offset] = '?';
				result[offset+1] = '\0';
                printf("IEC_STRING not handled yet");
				//strcpy( ((char*)result + index*STRING_LENGTH),value); // this should work though
				break;
			case IEC_TIME:
				// TODO: long long us
				sprintf(result + offset, "%lld", ((long long*)value)[index]);
				break;
			default:
				printf("Unknown type: %d\n", type);
				result[offset] = '-';
				result[offset+1] = '\0';
				break;
		}
		offset = strlen(result);
		if( arraylen > 1 && index < (arraylen-1))
		{
			result[offset] = ',';
			offset += 1;
		}
	}

	if( arraylen > 1 )
	{
		result[offset] = ']';
		offset += 1;
	}
	result[offset] = '\0';
}

void stringToVoid(const char* valueString, IEC61131Type type, int arraylen, void* result)
{
	char value[STRING_LENGTH];
	unsigned char c;
    signed char sc;
	int i;
	//unsigned int ui;
	long long l;
	double d;
	int offset = 0;
	int index;
	char *commaPos;

	for(index = 0; index < arraylen; index++)
	{
		if( arraylen > 1 )
		{
			offset += 1; // skip '[' or ','
			commaPos = strchr(valueString+offset, ',');
			if( commaPos != 0x0 )
			{
				*commaPos = '\0';
				strcpy(value, valueString+offset);
				value[(commaPos-valueString-offset)] = '\0';
				printf("Parse array value: %s\n",value);
				offset += (commaPos-(valueString+offset));
			}
			else
			{

				strcpy(value, valueString+offset);
				value[strlen(valueString+offset)-1] = '\0';
				offset += strlen(valueString+offset); // FIN

			}

		}
		else
			strcpy(value, valueString);

		switch(type)
		{
			case IEC_INT:
				i = atoi(value);
				((short*)result)[index] = i;
				break;
			case IEC_UINT:
				i = atoi(value);
				((unsigned short*)result)[index] = i;
				break;
			case IEC_BYTE:
			case IEC_USINT:
				c = atoi(value);
				((unsigned char*)result)[index] = c;
				break;
			case IEC_SINT:
				sc = atoi(value);
				((char*)result)[index] = sc;
				break;
			case IEC_CHAR:
				((unsigned char*)result)[index] = value[0];
				break;
			case IEC_DINT:
				i = atoi(value);
				((int*)result)[index] = i;
				break;
			case IEC_UDINT:
				i = atoi(value);
				((unsigned int*)result)[index] = i;
				break;
			case IEC_LINT:
				l = atol(value);
				((long long*)result)[index] = l;
				break;
			case IEC_ULINT:
				l = atol(value);
				((unsigned long long*)result)[index] = l;
				break;
			case IEC_REAL:
				d = atof(value);
				((float*)result)[index] = (float)d;
				break;
			case IEC_LREAL:
				d = atof(value);
				((double*)result)[index] = d;
				break;
			case IEC_BOOL:
				#ifdef ___ALTSIM___
					if( strcasecmp(value,"true") == 0)
				#else
					if( stricmp(value,"true") == 0)
				#endif
					((char*)result)[index] = 1;
				else //if( stricmp(value,"true") == 0)
					((char*)result)[index] = 0;
				break;
			case IEC_STRING:
			case IEC_WSTRING:
				strcpy( ((char*)result + index*STRING_LENGTH),value);
				break;
			case IEC_TIME:
				l = atol(value);
				((long long*)result)[index] = l;
				break;
			default:
				printf("Unknown type: %d\n", type);
				result = 0;
				break;
		}
	}
}


void appendConnectionToXML(mxml_node_t* simdata, char* source, char* destination){
    mxml_node_t* connection = mxmlNewElement(simdata, "Connection");
	mxmlElementSetAttr(connection, "Source", source);
	mxmlElementSetAttr(connection, "Destination", destination);
}

void appendEventOutputToXML(mxml_node_t* simdata, char* eventname){
    mxml_node_t* connection = mxmlNewElement(simdata, "Event");
	mxmlElementSetAttr(connection, "Name", eventname);
}

void appendVarToXML(mxml_node_t* simdata, char* varname, void* value, IEC61131Type vartype, int arraylen){
    char valstring[128];
	mxml_node_t* var = mxmlNewElement(simdata, "Var");

    voidToString(value, vartype, arraylen, valstring);

	mxmlElementSetAttr(var, "Name", varname);
	mxmlElementSetAttr(var, "Value", valstring); // TODO: too long?

}

// TODO: HCECC parent
void appendTransitionToXML(mxml_node_t* simdata, char* srcState, char* destState, char* condition){
	mxml_node_t* transition = mxmlNewElement(simdata, "ECTransition");

    mxmlElementSetAttr(transition, "Source", srcState);
	mxmlElementSetAttr(transition, "Destination", destState);
	mxmlElementSetAttr(transition, "Condition", condition);

}

//Used to Remove: Events / Transitions / Connections
void clearChildren(mxml_node_t* node){
	mxml_node_t* child;
	if( node->child == NULL ) return;
	child = node->child;
	deleteNexts(child);
	mxmlDelete(child);

}

void deleteNexts(mxml_node_t* child)
{
	if( child == NULL ) return;

	if( child->next != NULL )
	{
	    deleteNexts(child->next);
		mxmlDelete(child->next);
	}
}


	// Pretty Print (ISH)
const char *   whitespace_cb(mxml_node_t *node, int where)
{
	switch(where)
	{
		case MXML_WS_AFTER_OPEN:
			if( node->child != NULL )
				return "\t";
			else
				return NULL;
		case MXML_WS_BEFORE_OPEN:
			return "\n";
		case MXML_WS_AFTER_CLOSE:
			return NULL;
		case MXML_WS_BEFORE_CLOSE:
			return NULL;

	}

    return NULL;

}
