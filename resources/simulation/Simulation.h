#ifndef ___SIMULATION___
#define ___SIMULATION___

#include "mxml.h"

#include "fbtypes.h"

struct str_el
{
    char str[128]; // instead of char*
    bool inuse; // needed cause str may intensionally me empty :(
    struct str_el* next;
};

typedef struct str_el StringLinkedList;

typedef struct {
    char name[32];
} Event;


typedef struct {
    char name[32];
    char type[16];
    char arraysize[4];
    char initialValue[16];
} VarDeclaration;


typedef struct {
    int inputeventcount;
    Event EventInputs[20];
    int outputeventcount;
    Event EventOutputs[20];
    int inputvarcount;
    VarDeclaration InputVars[20];
    int outputvarcount;
    VarDeclaration OutputVars[20];
    char startstate[32];
} FunctionBlock;

typedef enum  {
    IEC_BOOL,
	IEC_BYTE,
    IEC_WORD,
    IEC_DWORD,
    IEC_LWORD,
    
	IEC_SINT, 
    IEC_USINT, 
	IEC_INT, 
	IEC_UINT, 
	IEC_DINT, 
	IEC_UDINT, 
	IEC_LINT, 
	IEC_ULINT, 
	
    IEC_REAL,
	IEC_LREAL,
    
    IEC_TIME,
    
    IEC_DATE,
    IEC_TIME_OF_DAY,
	IEC_DATE_AND_TIME,
	
	IEC_STRING, 
    IEC_WSTRING,
    IEC_CHAR // Not part of the standard (http://en.wikipedia.org/wiki/IEC_61131-3#Data_Types)
	
	
} IEC61131Type;

void appendTransitionToXML(mxml_node_t* simdata, char* srcState, char* destState, char* condition);
void appendConnectionToXML(mxml_node_t* simdata, char* src, char* dest);
void appendEventOutputToXML(mxml_node_t* simdata, char* eventName);
void appendVarToXML(mxml_node_t* simdata, char* varname, void* value, IEC61131Type vartype, int arraylen);
void clearChildren(mxml_node_t* simdata);
void voidToString(void* value, IEC61131Type type, int arraylen, char* result);
void stringToVoid(const char* value, IEC61131Type type, int arraylen, void* result);
void deleteNexts(mxml_node_t* child);

void appendTransitionToStatuses(mxml_node_t* simdata, char* srcState, char* destState, char* condition);
void appendEventOutputToStatuses(mxml_node_t* simdata, char* eventName);
void appendVarToStatuses(mxml_node_t* simdata, char* varname, void* value, char* vartype, int arraylen);

const char *   whitespace_cb(mxml_node_t *node, int where);

#endif
