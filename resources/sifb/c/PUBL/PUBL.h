// This file is generated by FBC

#ifndef PUBL@postfix@_H_
#define PUBL@postfix@_H_

#include "fbtypes.h"
#include "globalvars.h"
@includeUserTypes@

typedef union {
	UDINT events;
	struct {
		UDINT INIT : 1; // service initialization
		UDINT REQ : 1; // service request
	} event;
} PUBL@postfix@IEvents;

typedef union {
	UDINT events;
	struct {
		UDINT INITO : 1; // initialization confirm
		UDINT CNF : 1; // service confirmation
	} event;
} PUBL@postfix@OEvents;

typedef struct {
	BOOL _entered;
	PUBL@postfix@IEvents _input;
	@begin_varlist#SD_@
	@type#C@ _@var@;
	@end_varlist@
	PUBL@postfix@OEvents _output;
	@globalType@* _globalBuf;
} PUBL@postfix@;

/* PUBL@postfix@ initialization function */
void PUBL@postfix@init(PUBL@postfix@* me, @globalType@* global);

/* PUBL@postfix@ execution function */
void PUBL@postfix@run(PUBL@postfix@* me);

#endif /*PUBL@postfix@_H_*/
