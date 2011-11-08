#include "timespec.h"
#include <sys/time.h>

void timespec_now(struct timespec *ts)
{
    struct timeval  tv;

    // not using clock_gettime() [of rt library] due to portability
    gettimeofday(&tv, NULL);
    ts->tv_sec  = tv.tv_sec;
    ts->tv_nsec = tv.tv_usec*1000;  
}

void timespec_addms(struct timespec *ts, long ms)
{
    int sec=ms/1000;
    ms=ms-sec*1000;

    // perform the addition
    ts->tv_nsec+=ms*1000000;

    // adjust the time
    ts->tv_sec+=ts->tv_nsec/1000000000 + sec;
    ts->tv_nsec=ts->tv_nsec%1000000000;
}

void timespec_addns(struct timespec *ts, long ns)
{
    int sec=ns/1000000000;
    ns=ns - sec*1000000000;

    // perform the addition
    ts->tv_nsec+=ns;

    // adjust the time
    ts->tv_sec+=ts->tv_nsec/1000000000 + sec;
    ts->tv_nsec=ts->tv_nsec%1000000000;

}

int timespec_compare(struct timespec *a, struct timespec *b)
{
    if (a->tv_sec!=b->tv_sec)
        return a->tv_sec-b->tv_sec;
    return a->tv_nsec-b->tv_nsec;
}

void timespec_subtract(struct timespec *r, struct timespec *a, struct timespec *b)
{
    r->tv_sec = a->tv_sec;
    r->tv_nsec = a->tv_nsec - b->tv_nsec;
    if (r->tv_nsec < 0) {
        // borrow.
        r->tv_nsec += 1000000000;
        r->tv_sec --;
    }
    r->tv_sec = r->tv_sec - b->tv_sec;
}

long timespec_milliseconds(struct timespec *a) 
{
    return a->tv_sec*1000 + a->tv_nsec/1000000;
}
