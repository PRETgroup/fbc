CC = gcc
CFLAGS = -Wall -g -DDEBUG
TARGET = ASN1PUBSUBTest
LIBS = net

all: ASN1.o PUBLISH_8.o SUBSCRIBE_8.o subscriber.o publisher.o pubsub.o ASN1PUBSUBTest.c
	$(CC) $(CFLAGS) ASN1.o PUBLISH_8.o SUBSCRIBE_8.o subscriber.o publisher.o pubsub.o ASN1PUBSUBTest.c -o $(TARGET) -L $(LIBS)
	
localtest: ASN1.o ASN1Test.c
	$(CC) $(CFLAGS) ASN1.o ASN1Test.c -o ASN1Test

ASN1.o: ASN1.c ASN1.h
	$(CC) $(CFLAGS) -c ASN1.c -o ASN1.o
	
PUBLISH_8.o: PUBLISH_8.c PUBLISH_8.h publisher.o
	$(CC) $(CFLAGS) -c PUBLISH_8.c -o PUBLISH_8.o
	
SUBSCRIBE_8.o: SUBSCRIBE_8.c SUBSCRIBE_8.h subscriber.o
	$(CC) $(CFLAGS) -c SUBSCRIBE_8.c -o SUBSCRIBE_8.o
	
subscriber.o: subscriber.c pubsub.o
	$(CC) $(CFLAGS) -c subscriber.c -o subscriber.o
	
publisher.o: publisher.c pubsub.o
	$(CC) $(CFLAGS) -c publisher.c -o publisher.o

pubsub.o: pubsub.c pubsub.h
	$(CC) $(CFLAGS) -c pubsub.c -o pubsub.o
	
clean:
	rm *.o
	rm $(TARGET)
	rm ASN1Test