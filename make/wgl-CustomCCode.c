#include <stdio.h>

#define JOGL_DUMMY_WINDOW_NAME "__jogl_dummy_window"

LRESULT CALLBACK DummyWndProc( HWND   hWnd, UINT   uMsg, WPARAM wParam, LPARAM lParam) {
  return DefWindowProc(hWnd,uMsg,wParam,lParam);
}

ATOM oglClass = 0;

HWND CreateDummyWindow( int x, int y, int width, int height ) {
  HINSTANCE hInstance;
  DWORD     dwExStyle;
  DWORD     dwStyle;
  HWND      hWnd;

  hInstance = GetModuleHandle(NULL);
  if( !oglClass ) {
    WNDCLASS  wc;
    ZeroMemory( &wc, sizeof( wc ) );
    wc.style = CS_HREDRAW | CS_VREDRAW | CS_OWNDC;
    wc.lpfnWndProc = (WNDPROC) DummyWndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = hInstance;
    wc.hIcon = NULL;
    wc.hCursor = NULL;
    wc.hbrBackground = NULL;
    wc.lpszMenuName = NULL;
    wc.lpszClassName = JOGL_DUMMY_WINDOW_NAME;
    if( !(oglClass = RegisterClass( &wc )) ) {
      printf( "RegisterClass Failed: %d\n", GetLastError() );
      return( 0 );
    }
  }
  
  dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
  dwStyle = WS_OVERLAPPEDWINDOW;
  if( !(hWnd=CreateWindowEx( dwExStyle,
                             JOGL_DUMMY_WINDOW_NAME,
                             JOGL_DUMMY_WINDOW_NAME,
                             dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                             x, y, width, height,
                             NULL, NULL, hInstance, NULL ) ) ) {
    return( 0 );
  }
  return( hWnd );
}
