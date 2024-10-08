// This file is generated by FBC.

#include "E_CTU.h"
#include <string.h>

/* E_CTU initialization function */
void E_CTUinit(E_CTU* me)
{
    memset(me, 0, sizeof(E_CTU));
}

/* E_CTU execution function */
void E_CTUrun(E_CTU* me)
{
	me->_output.events = 0;

    if (me->_input.event.CU) {
        me->PV = me->_PV;
    }
	
	if( me->_input.event.R )
	{
		me->_CV = 0;
		me->_Q = false;
		me->_output.event.RO = 1;
	}
	
	if( me->_input.event.CU )
	{
		me->_CV++;
		if( me->_CV > me->PV )
			me->_Q = true;
		else
			me->_Q = false;
        me->_output.event.CUO = 1;
	}
    
    me->_input.events = 0;
}
