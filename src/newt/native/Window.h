
#ifndef _WINDOW_H_
#define _WINDOW_H_

#define FLAG_CHANGE_PARENTING       ( 1 <<  0 )
#define FLAG_CHANGE_DECORATION      ( 1 <<  1 )
#define FLAG_CHANGE_FULLSCREEN      ( 1 <<  2 )
#define FLAG_CHANGE_ALWAYSONTOP     ( 1 <<  3 )
#define FLAG_CHANGE_VISIBILITY      ( 1 <<  4 )

#define FLAG_HAS_PARENT             ( 1 <<  8 )
#define FLAG_IS_UNDECORATED         ( 1 <<  9 )
#define FLAG_IS_FULLSCREEN          ( 1 << 10 )
#define FLAG_IS_ALWAYSONTOP         ( 1 << 11 )
#define FLAG_IS_VISIBLE             ( 1 << 12 )

#define TST_FLAG_CHANGE_PARENTING(f)   ( 0 != ( (f) & FLAG_CHANGE_PARENTING ) ) 
#define TST_FLAG_CHANGE_DECORATION(f)  ( 0 != ( (f) & FLAG_CHANGE_DECORATION ) ) 
#define TST_FLAG_CHANGE_FULLSCREEN(f)  ( 0 != ( (f) & FLAG_CHANGE_FULLSCREEN ) ) 
#define TST_FLAG_CHANGE_ALWAYSONTOP(f) ( 0 != ( (f) & FLAG_CHANGE_ALWAYSONTOP ) ) 
#define TST_FLAG_CHANGE_VISIBILITY(f)  ( 0 != ( (f) & FLAG_CHANGE_VISIBILITY ) ) 

#define TST_FLAG_HAS_PARENT(f)        ( 0 != ( (f) & FLAG_HAS_PARENT ) ) 
#define TST_FLAG_IS_UNDECORATED(f)    ( 0 != ( (f) & FLAG_IS_UNDECORATED ) ) 
#define TST_FLAG_IS_FULLSCREEN(f)     ( 0 != ( (f) & FLAG_IS_FULLSCREEN ) ) 
#define TST_FLAG_IS_FULLSCREEN(f)     ( 0 != ( (f) & FLAG_IS_FULLSCREEN ) ) 
#define TST_FLAG_IS_ALWAYSONTOP(f)    ( 0 != ( (f) & FLAG_IS_ALWAYSONTOP ) ) 
#define TST_FLAG_IS_VISIBLE(f)        ( 0 != ( (f) & FLAG_IS_VISIBLE ) ) 

#endif
