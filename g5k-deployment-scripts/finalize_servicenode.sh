#!/bin/bash
site=`tail  -n 1 ../debug/hosts.list`
head -n -1 ../debug/hosts.list > worker_nodes.txt 
scp worker_nodes.txt frontend.nancy:/home/alebre/load_injector_experiment/config/.
ssh root@$site " cd /home/alebre/load_injector_experiment/ ; rm ./exp.traces ; rm all* ; rm logs.txt ; rm -rf ./logs/*"
