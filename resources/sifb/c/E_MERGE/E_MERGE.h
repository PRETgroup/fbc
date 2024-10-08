// This file is generated by FBC.

#ifndef E_MERGE_H_
#define E_MERGE_H_

#include "fbtypes.h" // for bool

typedef union {
    UDINT events;
    struct {
        UDINT EI1 : 1; // Ev1
        UDINT EI2 : 1; // Ev2
    } event;
} E_MERGEIEvents;

typedef union {
    UDINT events;
    struct {
        UDINT EO : 1; // Event Out
    } event;
} E_MERGEOEvents;

typedef struct {
    UINT _state;
    BOOL _entered;

    E_MERGEIEvents _input;
    E_MERGEOEvents _output;

} E_MERGE;

/* E_MERGE initialization function */
void E_MERGEinit(E_MERGE* me);

/* E_MERGE execution function */
void E_MERGErun(E_MERGE* me);

#endif	// E_MERGE_H_
