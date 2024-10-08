// This file is generated by FBC.

#ifndef WAGOOUTPUTBLOCK_H_
#define WAGOOUTPUTBLOCK_H_

#include "fbtypes.h"
#include "kbusapi.h"
@addObjDependency#kbusapi.o

typedef union {
    UDINT events;
    struct {
        UDINT INIT : 1; // Initialization Request
        UDINT REQ : 1; // Execution Confirmation
    } event;
} WagoOutputBlockIEvents;

typedef union {
    UDINT events;
    struct {
        UDINT CNF : 1; // Execution Confirmation
    } event;
} WagoOutputBlockOEvents;

// Basic Function Block Type
typedef struct {
    UINT _state;
    BOOL _entered;
    WagoOutputBlockIEvents _input;
    INT ByteIndex;
    INT _ByteIndex;
    BOOL DO0;
    BOOL _DO0;
    BOOL DO1;
    BOOL _DO1;
    BOOL DO2;
    BOOL _DO2;
    BOOL DO3;
    BOOL _DO3;
    BOOL DO4;
    BOOL _DO4;
    BOOL DO5;
    BOOL _DO5;
    BOOL DO6;
    BOOL _DO6;
    BOOL DO7;
    BOOL _DO7;
    WagoOutputBlockOEvents _output;
} WagoOutputBlock;

/* Function block initialization function */
void WagoOutputBlockinit(WagoOutputBlock* me);

/* Function block execution function */
void WagoOutputBlockrun(WagoOutputBlock* me);

/* ECC algorithms */
void WagoOutputBlock_SetKBusInputs(WagoOutputBlock* me);

#endif // WAGOOUTPUTBLOCK_H_
