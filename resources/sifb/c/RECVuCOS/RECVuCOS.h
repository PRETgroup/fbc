// This file is generated by FBC.

#ifndef RECVUCOS@postfix@_H_
#define RECVUCOS@postfix@_H_

#include "fbtypes.h"
@includeUserTypes@

typedef union {
    UDINT events;
    struct {
        UDINT IND : 1; // Service Indication
    } event;
} RECVuCOS@postfix@OEvents;

typedef struct {
    BOOL _entered;
    RECVuCOS@postfix@OEvents _output;
    @begin_varlist#RD_@
    @type#C@ _@var@;
    @end_varlist@
    unsigned char (*_readMsg)(void* buf);
} RECVuCOS@postfix@;

/* Function block initialization function */
void RECVuCOS@postfix@init(RECVuCOS@postfix@* me, unsigned char (*rMsg)(void* buf));

/* Function block execution function */
void RECVuCOS@postfix@run(RECVuCOS@postfix@* me);

#endif // RECVUCOS@postfix@_H_

