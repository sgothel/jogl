#import "ContextUpdater.h"
#import <pthread.h>

@implementation ContextUpdater
{
}

static pthread_mutex_t resourceLock = PTHREAD_MUTEX_INITIALIZER;

static void printLockDebugInfo(char *message, char *func, int line)
{
    fprintf(stderr, "%s in function: \"%s\" at line: %d\n", message, func, line);
    fflush(NULL);
}

- (void) lock
{
    pthread_mutex_lock(&resourceLock);
}

- (void) lockInFunction:(char *)func atLine:(int)line
{
    printLockDebugInfo("locked  ", func, line);
    [self lock];
}

- (void) unlock
{
    pthread_mutex_unlock(&resourceLock);
}

- (void) unlockInFunction:(char *)func atLine:(int)line
{
    printLockDebugInfo("unlocked", func, line);
    [self unlock];
}

- (void) update:(NSNotification *)notification
{
    [self lock];
    
    NSRect r = [view frame];
    if(viewRect.origin.x != r.origin.x || 
       viewRect.origin.y != r.origin.y || 
       viewRect.size.width != r.size.width ||
       viewRect.size.height != r.size.height) {
        viewUpdated = TRUE;
        viewRect = r;
    }

    [self unlock];
}

- (BOOL) needsUpdate
{
    BOOL r;
    [self lock];
    
    r = viewUpdated;
    viewUpdated = FALSE;
    
    [self unlock];

    return r;
}

- (id) initWithContext:(NSOpenGLContext *)context view: (NSView *)nsView
{
    ctx = context;
    view = nsView;
    [ctx retain];
    [view retain];
    viewRect = [view frame];
    viewUpdated = TRUE;
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(update:) name:NSViewGlobalFrameDidChangeNotification object: view];

    return [super init];
}

- (void) dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [view release];
    [ctx release];
    
    [super dealloc];
}

@end
