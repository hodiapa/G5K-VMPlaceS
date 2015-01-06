all: memtouch-with-busyloop memtouch-ajust-sleep

memtouch-with-busyloop: memtouch-with-busyloop3.c
	gcc -O2 -lm -std=gnu99 -Wall memtouch-with-busyloop3.c -o memtouch-with-busyloop3

memtouch-ajust-sleep: memtouch-with-busyloop3-with-target-cpu-adjust-sleep.c
	gcc -O2 -lm -std=gnu99 -Wall memtouch-with-busyloop3-with-target-cpu-adjust-sleep.c -o memtouch-adjust-sleep
         
clean:
	@echo "Deleting tmp files"
	@rm -f memtouch-with-busyloop3 memtouch-adjust-sleep *.o
