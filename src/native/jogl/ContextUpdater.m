#import "ContextUpdater.h"
#import <pthread.h>

@implementation ContextUpdater
{
}

static NSOpenGLContext *theContext;
static pthread_mutex_t resourceLock = PTHREAD_MUTEX_INITIALIZER;

static void printLockDebugInfo(char *message, char *func, int line)
{
	fprintf(stderr, "%s in function: \"%s\" at line: %d\n", message, func, line);
	fflush(stderr);
}

+ (void) lock
{
	if (theContext != NULL)
	{
		pthread_mutex_lock(&resourceLock);
	}
}

+ (void) lockInFunction:(char *)func atLine:(int)line
{
	if (theContext != NULL)
	{
		printLockDebugInfo("locked  ", func, line);
		[self lock];
	}
}

+ (void) unlock
{
	if (theContext != NULL)
	{
		pthread_mutex_unlock(&resourceLock);
	}
}

+ (void) unlockInFunction:(char *)func atLine:(int)line
{
	if (theContext != NULL)
	{
		printLockDebugInfo("unlocked", func, line);
		[self unlock];
	}
}

- (void) registerFor:(NSOpenGLContext *)context with: (NSView *)view
{
	if (view != NULL)
	{
		[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(update:) name:NSViewGlobalFrameDidChangeNotification object: view];
		theContext = context;
	}
}

- (void) update:(NSNotification *)notification
{
	[ContextUpdater lock];
	
	[theContext update];
	
	[ContextUpdater unlock];
}

- (id) init
{	
	theContext = NULL;
	
	return [super init];
}

- (void) dealloc
{
	[[NSNotificationCenter defaultCenter] removeObserver:self];
	
	[super dealloc];
}

@end