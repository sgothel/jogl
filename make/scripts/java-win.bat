
REM set MODULE_ARGS=--enable-native-access=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED
set MODULE_ARGS=--enable-native-access=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED --add-opens java.desktop/sun.awt.windows=ALL-UNNAMED --add-opens java.desktop/sun.java2d=ALL-UNNAMED

REM %J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %D_ARGS% %MODULE_ARGS% %X_ARGS% %* > java-win.log 2>&1
%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% %MODULE_ARGS% %X_ARGS% %* > java-win.log 2>&1
tail java-win.log

