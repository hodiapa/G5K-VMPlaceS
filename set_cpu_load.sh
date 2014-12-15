#!/bin/bash                                                                     

# cleaning the running stress processes                                         
pids=$(ps aux | grep stress | grep -v "grep" | awk '{print $2}')

for pid in $pids; do
    echo "killing the previous process $pid"
    kill -9 $pid
done

# launching a new stress process                                                
stress --cpu 1 &

pids=$(ps aux | grep stress | grep -v "grep" | awk '{print $2}')

# limiting the stress processes                                                 
for pid in $pids; do
    echo "limiting to $1% the process $pid"
    cpulimit -p $pid -l $1 &
done