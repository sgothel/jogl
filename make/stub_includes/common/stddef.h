#if defined(_WIN64)
    typedef __int64 ptrdiff_t;
#elif defined(__ia64__) || defined(__x86_64__)
    typedef long int ptrdiff_t;
#else
    typedef int ptrdiff_t;
#endif
