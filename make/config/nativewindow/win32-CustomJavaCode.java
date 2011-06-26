  
  private static final boolean DEBUG = Debug.debug("GDI");

  private static final String dummyWindowClassNameBase = "_dummyWindow_clazz" ;
  private static RegisteredClassFactory dummyWindowClassFactory;
  private static boolean isInit = false;

  private static native boolean initIDs0();
  private static native long getDummyWndProc0();

  public static synchronized void initSingleton(boolean firstX11ActionOnProcess) {
    if(!isInit) {
        NWJNILibLoader.loadNativeWindow("win32");
        
        if( !initIDs0() ) {
            throw new NativeWindowException("GDI: Could not initialized native stub");
        }

        if(DEBUG) {
            System.out.println("GDI.isFirstX11ActionOnProcess: "+firstX11ActionOnProcess);
        }

        dummyWindowClassFactory = new RegisteredClassFactory(dummyWindowClassNameBase, getDummyWndProc0());
        isInit = true;
    }
  }

  public static boolean requiresToolkitLock() { return false; }

  private static RegisteredClass dummyWindowClass = null;
  private static Object dummyWindowSync = new Object();

  public static long CreateDummyWindow(int x, int y, int width, int height) {
      synchronized(dummyWindowSync) {
          dummyWindowClass = dummyWindowClassFactory.getSharedClass();
          return CreateDummyWindow0(dummyWindowClass.getHandle(), dummyWindowClass.getName(), dummyWindowClass.getName(), x, y, width, height);
      }
  }

  public static boolean DestroyDummyWindow(long hwnd) {
      boolean res;
      synchronized(dummyWindowSync) {
          if( null == dummyWindowClass ) {
              throw new InternalError("GDI Error ("+dummyWindowClassFactory.getSharedRefCount()+"): SharedClass is null");
          }
          res = DestroyWindow(hwnd);
          dummyWindowClassFactory.releaseSharedClass();
      }
      return res;
  }

  public static Point GetRelativeLocation(long src_win, long dest_win, int src_x, int src_y) {
      return (Point) GetRelativeLocation0(src_win, dest_win, src_x, src_y);
  }
  private static native Object GetRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);

  public static native boolean CreateWindowClass(long hInstance, String clazzName, long wndProc);
  public static native boolean DestroyWindowClass(long hInstance, String className);
  static native long CreateDummyWindow0(long hInstance, String className, String windowName, int x, int y, int width, int height);

