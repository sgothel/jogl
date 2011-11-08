#ifndef _timespec_h
#define _timespec_h

#include <time.h>

void timespec_now(struct timespec *ts);
void timespec_addms(struct timespec *ts, long ms);
void timespec_addns(struct timespec *ts, long ns);

/** returns 0: a==b, >0: a>b, <0: a<b */
int timespec_compare(struct timespec *a, struct timespec *b);

/** computes r = a - b */
void timespec_subtract(struct timespec *r, struct timespec *a, struct timespec *b);

/** convert the timespec into milliseconds (may overflow) */
long timespec_milliseconds(struct timespec *a);

#endif /* _timespec_h */
