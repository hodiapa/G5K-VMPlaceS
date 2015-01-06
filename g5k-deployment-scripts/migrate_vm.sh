
#!/bin/bash

# Filename: migrate_fromentropy.sh
# Description: 
# Author: Flavien Quesnel
# Maintainer: 
# Created: mer. mars 14 17:00:52 2012 (+0100)
# Version: 
# Last-Updated: Sep 18 15:50:50 CEST 2013 
#           By:  Adrien
#     Update #: Comment storage related stuff
# URL: 

## Perform a live migration of a virtual machine to a destination host
## Creation of a backing if not present
#echo "usage: $0 NOM_VM DESTINATION_HOST" 

VM=$1
DEST=$2
MIG_SPEED=$3
POOL_DIR=/tmp

#### Check if the VM(backing file) exists on the destination host
#ssh root@$DEST -o StrictHostKeyChecking=no "test -e $POOL_DIR/$VM.qcow2"
#TEST=$? # 0 : the VM exists || 1 : the VM doesn't exists
#
#if [ $TEST = 1 ];
#then
#    ## Create a backing file on the destination host
#    ssh root@$DEST -o StrictHostKeyChecking=no "qemu-img create -f qcow2 -o backing_file=$POOL_DIR/vm-base.img,backing_fmt=raw $POOL_DIR/$VM.qcow2"
#fi
#
### Live migration 
#virsh migrate $VM --live --copy-storage-inc qemu+tcp://$DEST/system 2>&1 | tee -a /root/migrate.log

virsh --connect qemu:///system migrate-setspeed $VM $MIG_SPEED
#virsh --connect qemu:///system migrate $VM --live --copy-storage-inc --timeout 600 qemu+tcp://$DEST/system 2>&1 | tee -a /root/migrate.log
/usr/bin/time -f "%e" virsh --connect qemu:///system migrate $VM --live --copy-storage-inc --timeout 600 qemu+tcp://$DEST/system 2>&1 #| tee -a /root/migrate.log

