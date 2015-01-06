#!/bin/bash

cal_cpu_speed=$1
cal_mem_speed=$2

mem_size=$3
mem_speed=$4

# cleaning the running memtouch and cpulimit processes
ps aux | grep "memtouch-with-busyloop3" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9
ps aux | grep "cpulimit" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9

# launching a new and clean memtouch process
nohup memtouch-with-busyloop3 --cmd-makeload --cpu-speed $cal_cpu_speed --mem-speed $cal_mem_speed $mem_size $mem_speed > /dev/null 2>&1 &


# waiting for the memtouch to start (asynchronously)
memtouch_pid=$(ps aux | grep "memtouch-with-busyloop3" | grep -v "grep" | awk '{print $2}')

while [ "$memtouch_pid" == "" ]; do
    sleep 1
    memtouch_pid=$(ps aux | grep "memtouch-with-busyloop3" | grep -v "grep" | awk '{print $2}')
done


if [ "$memtouch_pid" != "" ]; then

    # cleaning the running cpulimit processes
    ps aux| grep "cpulimit" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9

    echo "memtouch process is running: pid=$memtouch_pid, the new targeted cpu load is 1% of one core."
    nohup cpulimit -l 1 -p $memtouch_pid > /dev/null 2>&1 &
else
    echo "cannot find a running memtouch process."
fi

exit 0
