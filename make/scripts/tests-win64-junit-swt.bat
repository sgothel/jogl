set THISDIR=%cd%

set SDIR=%~dp0%

%SDIR%/make.jogl.all.win64.bat -f build-test.xml junit.run.settings junit.run.swt.awt


