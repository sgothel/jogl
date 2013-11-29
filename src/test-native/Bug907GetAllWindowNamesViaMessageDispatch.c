#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#undef WIN32_LEAN_AND_MEAN

#include <wingdi.h>
#include <stddef.h>
#include <stdio.h>

BOOL CALLBACK EnumWindowsProc(HWND hwnd, LPARAM lParam)
{
 static int i = 0;
 char buffer[255];
 BOOL bRet = SendMessageTimeout(hwnd, WM_GETTEXT, 255, (LPARAM)buffer,
				SMTO_ABORTIFHUNG, 1000/*ms*/, NULL);
 if(bRet == 0) {
   fprintf(stderr,"#%4d: FAILURE!\n", i++); fflush(stderr);
   return FALSE;
 } else {
   fprintf(stderr,"#%4d: GOT: %s\n", i++, buffer); fflush(stderr);
   return TRUE;
 }
}

int main(int argc, char **argv)
{
 BOOL bRet = EnumWindows(EnumWindowsProc, 0);
 if(bRet == 0)
   {
     fprintf(stderr,"ERROR!");
     exit(EXIT_FAILURE);
   }
 fprintf(stderr,"SUCCESS!");
 exit(EXIT_SUCCESS);
}
