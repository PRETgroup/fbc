// This file is generated by FBC.

#ifndef SENDUCOS@postfix@_H_
#define SENDUCOS@postfix@_H_

#include "fbtypes.h"
@includeUserTypes@

typedef union {
    UDINT events;
    struct {
        UDINT REQ : 1; // Service Request
    } event;
} SENDuCOS@postfix@IEvents;

typedef union {
    UDINT events;
    struct {
        UDINT CNF : 1; // Service Confirmation
    } event;
} SENDuCOS@postfix@OEvents;

typedef struct {
    BOOL _entered;
    SENDuCOS@postfix@IEvents _input;
	UINT _sample;
    @begin_varlist#SD_@
    @type#C@ _@var@;
    @end_varlist@
    SENDuCOS@postfix@OEvents _output;
	BOOL hasMsg;
    void (*_sendMsg)(void* msg, UINT sample);
    void (*_sendNull)(UINT sample);
} SENDuCOS@postfix@;

/* Function block initialization function */
void SENDuCOS@postfix@init(SENDuCOS@postfix@* me, void (*sMsg)(void* msg, UINT sample), void (*sNull)(UINT sample));

/* Function block execution function */
void SENDuCOS@postfix@run(SENDuCOS@postfix@* me);

/* Delayed send function */
void SENDuCOS@postfix@send(SENDuCOS@postfix@* me);

#endif // SENDUCOS@postfix@_H_
