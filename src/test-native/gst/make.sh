gcc -Wall helloworld.c -o helloworld $(pkg-config --cflags --libs gstreamer-0.10)
gcc -Wall helloworld-auto.c -o helloworld-auto $(pkg-config --cflags --libs gstreamer-0.10)
gcc -Wall helloworld-playbin.c -o helloworld-playbin $(pkg-config --cflags --libs gstreamer-0.10)
gcc -Wall helloworld-playbin2.c -o helloworld-playbin2 $(pkg-config --cflags --libs gstreamer-0.10)

