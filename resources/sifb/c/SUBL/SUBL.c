// This file is generated by FBC.

#include "SUBL@postfix@.h"
#include <string.h>

/* SUBL@postfix@ initialization function */
void SUBL@postfix@init(SUBL@postfix@* me, @globalType@* global)
{
    me->_entered = false;
    me->_input.events = 0;
    me->_output.events = 0;
    me->_globalBuf = global;
}

/* SUBL@postfix@ execution function */
void SUBL@postfix@run(SUBL@postfix@* me)
{
	me->_output.events = 0;

	if (!me->_entered) {
		me->_entered = true;
	}
	else {
		if (me->_input.event.INIT) {
			me->_output.event.INITO = 1;
		}
		else if (me->_globalBuf->status & _NEW) {
			memmove(&me->_RD_@indexRange#1@, &me->_globalBuf->buffer.data_@indexRange#1@, sizeof(me->_RD_@indexRange#1@));
			me->_globalBuf->status &= ~_NEW;
			me->_output.event.IND = 1;
		}
	}

	me->_input.events = 0;
}
