// This file is generated by FBC.
// COMMCHANNEL stuff done in COMMCHANNEL.. this is just a place holder
#include "LINKDATARECEIVER@unique@.h"
#include <string.h>

#include "COMMCHANNELUDPRECV.h"

/* LINKDATARECEIVER initialization function */
void LINKDATARECEIVER@unique@init(LINKDATARECEIVER@unique@* me)
{
    memset(me, 0, sizeof(LINKDATARECEIVER@unique@));
}

/* LINKDATARECEIVER@unique@ execution function */
void LINKDATARECEIVER@unique@run(LINKDATARECEIVER@unique@* me)
{
	me->_output.events = 0;

	if (me->_input.event.INIT) {
		me->_QO = me->_QI;
		// Register with COMMCHANNELUDPRECV
		LINKDATARECEIVER@unique@_Register(me);
		me->_output.event.INITO = 1;
	}
	
	me->_input.events = 0;
	
}

/****************************************************
 VARIABLE
 ****************************************************/
@begin_varlist#DATAWIREIDS@
extern void* DATA_@value@;
@end_varlist@
	
void LINKDATARECEIVER@unique@_Register(LINKDATARECEIVER@unique@* receiver)
{
	// Variable
	@begin_varlist#DATAWIREIDS@
	DATA_@value@ = &receiver->_RD_@var@;
	@end_varlist@
}