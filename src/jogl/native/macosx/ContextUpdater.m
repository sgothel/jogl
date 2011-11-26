#import "ContextUpdater.h"
#import <pthread.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) NSLog(@ __VA_ARGS__)
    // #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

#ifndef CGL_VERSION_1_3
    #warning this SDK doesn't support OpenGL profile
#endif

@implementation ContextUpdater
{
}

- (void) update:(NSNotification *)notification
{
    pthread_mutex_lock(&resourceLock);
    
    NSRect r = [view frame];
    if(viewRect.origin.x != r.origin.x || 
       viewRect.origin.y != r.origin.y || 
       viewRect.size.width != r.size.width ||
       viewRect.size.height != r.size.height) {
        viewUpdated = TRUE;
        viewRect = r;
    }

    pthread_mutex_unlock(&resourceLock);
}

- (BOOL) needsUpdate
{
    BOOL r;
    pthread_mutex_lock(&resourceLock);
    
    r = viewUpdated;
    viewUpdated = FALSE;
    
    pthread_mutex_unlock(&resourceLock);

    return r;
}

- (id) initWithContext:(NSOpenGLContext *)context view: (NSView *)nsView
{
    DBG_PRINT("ContextUpdater::init.0 view %p, ctx %p\n", view, ctx);
    pthread_mutex_init(&resourceLock, NULL); // fast non-recursive
    ctx = context;
    view = nsView;
    [ctx retain];
    [view retain];
    viewRect = [view frame];
    viewUpdated = TRUE;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(update:) name:NSViewGlobalFrameDidChangeNotification object: view];
    DBG_PRINT("ContextUpdater::init.X\n");

    return [super init];
}

- (void) dealloc
{
    DBG_PRINT("ContextUpdater::dealloc.0 view %p, ctx %p\n", view, ctx);
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [view release];
    [ctx release];
    pthread_mutex_destroy(&resourceLock);
    DBG_PRINT("ContextUpdater::dealloc.X\n");
    
    [super dealloc];
}

@end
