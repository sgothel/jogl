  
  static {
    NWJNILibLoader.loadNativeWindow("win32");
    
    if( !initIDs0() ) {
        throw new NativeWindowException("GDI: Could not initialized native stub");
    }
  }

  public static synchronized void initSingleton() {
  }
  private static native boolean initIDs0();

  private static Object createDummyWindowSync = new Object();

  public static long CreateDummyWindow(int x, int y, int width, int height) {
      synchronized(createDummyWindowSync) {
          return CreateDummyWindow0(x, y, width, height);
      }
  }

  public static Point GetRelativeLocation(long src_win, long dest_win, int src_x, int src_y) {
      return (Point) GetRelativeLocation0(src_win, dest_win, src_x, src_y);
  }
  private static native Object GetRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);

