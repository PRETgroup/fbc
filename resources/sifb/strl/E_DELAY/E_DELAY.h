// This file is generated by FBC.

#ifndef E_DELAY_H_
#define E_DELAY_H_

#include <sys/time.h>

typedef struct {
    int DT;
    struct timeval startTime;
} EDelay;
void EDelayinit(EDelay* me, int DT);
int EDelaycheck(EDelay me);


#endif	// E_DELAY_H_
