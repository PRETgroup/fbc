// This file is generated by FBC.

#include "RECVuCOS@postfix@.h"
#include "includes.h"

/* Function block initialization function */
void RECVuCOS@postfix@init(RECVuCOS@postfix@* me, unsigned char (*rMsg)(void* buf))
{
    me->_entered = false;
    me->_output.events = 0;
    me->_readMsg = rMsg;
}

/* Function block execution function */
void RECVuCOS@postfix@run(RECVuCOS@postfix@* me)
{
    me->_output.events = 0;

    // State: START
    if (!me->_entered) {
        me->_entered = true;
    }
    else {
		OSSchedLock();
        if ( (*me->_readMsg)(&me->_RD_1) ) {
            me->_output.event.IND = 1;
        }
		OSSchedUnlock();
    }

}
