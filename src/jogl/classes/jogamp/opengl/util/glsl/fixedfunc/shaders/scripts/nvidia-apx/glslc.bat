REM 
REM You have to call it from the 'shaders' directory, e.g.:
REM   scripts\nvidia-apx\glslc.bat <FileName>
REM
IF !"%JOGLDIR%"==""! GOTO YESPATH
set JOGLDIR=..\lib
:YESPATH

java -cp %JOGLDIR%\jogl.core.jar;%JOGLDIR%\jogl.gles2.jar;%JOGLDIR%\jogl.fixed.jar;%JOGLDIR%\jogl.sdk.jar com.jogamp.opengl.util.glsl.sdk.CompileShaderNVidia %1
