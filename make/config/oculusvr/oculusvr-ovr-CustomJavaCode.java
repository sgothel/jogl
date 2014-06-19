
static {
    AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                public DynamicLibraryBundle run() {
                    final DynamicLibraryBundle bundle =  new DynamicLibraryBundle(new OVRDynamicLibraryBundleInfo());
                    if(null==bundle) {
                      throw new RuntimeException("Null DynamicLibraryBundle");
                    }
                    /** No native tool library to load
                    if(!bundle.isToolLibLoaded()) {
                      throw new RuntimeException("Couln't load native OVR library");
                    } */
                    if(!bundle.isLibComplete()) {
                      throw new RuntimeException("Couln't load native OVR/JNI glue library");
                    }
                    if( !initializeImpl() ) {
                        throw new RuntimeException("Initialization failure");
                    }
                    return bundle;
                } } );
}

