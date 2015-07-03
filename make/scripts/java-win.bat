
REM %J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" %D_ARGS% %X_ARGS% %* > java-win.log 2>&1
%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% %X_ARGS% %* > java-win.log 2>&1
tail java-win.log

