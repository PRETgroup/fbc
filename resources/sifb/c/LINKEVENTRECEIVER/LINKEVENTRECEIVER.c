// This file is generated by FBC.
// COMMCHANNEL stuff done in COMMCHANNEL.. this is just a place holder
#include "LINKEVENTRECEIVER@unique@.h"
#include <string.h>

/* LINKEVENTRECEIVER@unique@ initialization function */
void LINKEVENTRECEIVER@unique@init(LINKEVENTRECEIVER@unique@* me)
{
    memset(me, 0, sizeof(LINKEVENTRECEIVER@unique@));
}

/* LINKEVENTRECEIVER@unique@ execution function */
void LINKEVENTRECEIVER@unique@run(LINKEVENTRECEIVER@unique@* me)
{
	if (me->_input.event.INIT) {
		me->_QO = me->_QI;
		// Register with COMMCHANNELUDPRECV
		LINKEVENTRECEIVER@unique@_Register(me);
	}
	
	me->_output.event.INITO = me->_input.event.INIT;
    if( me->_preoutput.event.INITO )
        me->_output.event.INITO = 0;
	/*for(Event e: fb.outputEvents)
			{
			cprinter.write("    if( me->_preoutput.event."+e.getName()+" )\n");
			cprinter.write("        me->_output.event."+e.getName()+" = 0;\n");
			}
	*/
	if( me->_preoutput.event.IND_1 )
		me->_output.event.IND_1 = 0;
    me->_preoutput.events =  me->_output.events;
	
}

/*******************************************************
  VARIABLE
 *******************************************************/
 @begin_varlist#EVENTWIREIDS@
 extern OutputEvent EVENT_@value@;
 @end_varlist@
 
void LINKEVENTRECEIVER@unique@_Register(LINKEVENTRECEIVER@unique@* receiver)
{
	// Variable
	@begin_varlist#EVENTWIREIDS@
	EVENT_@value@.events = &receiver->_output.events;
	EVENT_@value@.eventNum = @var@;
	@end_varlist@
}