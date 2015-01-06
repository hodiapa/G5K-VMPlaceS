#!/bin/bash

TMPFILE_SIZE=$1
TMPFS_SIZE=$(($1 + 10))m

echo "usage: init_cpu_load.sh: $TMPFILE_SIZE"
# remount tmpfs with enough size
echo "remount with $TMPFS_SIZE"
mount -t tmpfs none /dev/shm -o remount,defaults,size=$TMPFS_SIZE

echo "turn swapoff"
swapoff -a

# cleaning the running memtouch and cpulimit processes
ps aux | grep "memtouch-adjust-sleep" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9

# launching a new and clean memtouch process
nohup memtouch-adjust-sleep  --cmd-calibrate $TMPFILE_SIZE --mempath /dev/shm/memtouch > /dev/null 2>&1 &


# waiting for the memtouch to start (asynchronously)
sleep 1
memtouch_pid=$(ps aux | grep "memtouch-adjust-sleep" | grep -v "grep" | awk '{print $2}')

while [ "$memtouch_pid" != "" ]; do
    sleep 1
    memtouch_pid=$(ps aux | grep "memtouch-adjust-sleep" | grep -v "grep" | awk '{print $2}')
    echo "memtouch is  currently calibrating the system, please wait"
done

exit 0
