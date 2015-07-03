#  -I/usr/include/libavcodec -I/usr/include/libavformat \

gcc \
  -o avcodec_sample avcodec_sample.c \
  -lavformat -lavcodec -lavutil -lswscale

