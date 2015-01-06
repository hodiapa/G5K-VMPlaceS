#!/bin/sh
S=1

TMP=`grep br0 /proc/net/dev | sed -e 's/:/ /' `
INIT_IN=`echo $TMP | awk '{print $2}'`
INIT_OUT=`echo $TMP | awk '{print $10}'`

while true ; do
        sleep $S
        TMP=`grep br0 /proc/net/dev | sed -e 's/:/ /' `
        LAST_IN=`echo $TMP | awk '{print $2}'`
        LAST_OUT=`echo $TMP | awk '{print $10}'`
        echo "`date +%s --utc` `expr \( $LAST_IN - $INIT_IN \) / $S` `expr \( $LAST_OUT - $INIT_OUT \) / $S`"
        INIT_IN=$LAST_IN
        INIT_OUT=$LAST_OUT
done
