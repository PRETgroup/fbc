// This file is generated by FBC.

#include "PUBL@postfix@.h"
#include <string.h>

/* PUBL@postfix@ initialization function */
void PUBL@postfix@init(PUBL@postfix@* me, @globalType@* global)
{
    me->_entered = false;
    me->_input.events = 0;
    me->_output.events = 0;
    me->_globalBuf = global;
}

/* PUBL@postfix@ execution function */
void PUBL@postfix@run(PUBL@postfix@* me)
{
	me->_output.events = 0;
	
	if (!me->_entered) {
		me->_entered = true;
	}
	else {
		if (me->_input.event.INIT) {
			me->_output.event.INITO = 1;
		}
		else if (me->_input.event.REQ) {
			me->_globalBuf->status = _NEW;
			memmove(&me->_globalBuf->buffer.data_@indexRange#1@, &me->_SD_@indexRange#1@, sizeof(me->_SD_@indexRange#1@));
			me->_output.event.CNF = 1;
		}
	}
	
	me->_input.events = 0;
}
