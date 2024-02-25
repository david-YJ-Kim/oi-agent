#!/bin/bash

# Set Process profile
profile=$1

# Declare process name.
readonly PROC_NAME="mes-oia-$profile"
readonly PID_PATH="./"
readonly PROC_PID="${PID_PATH}${PROC_NAME}.pid"

# 중지
stop()
{
    echo "Stopping ${PROC_NAME}..."
    local DAEMON_PID=`cat "${PROC_PID}"`
    local PID=$(get_status)
    echo "DAEMON_PID=${DAEMON_PID} : PID=${PID}"

    if [ -n "${PID}" ]; then
        kill -15 $PID
        rm -f $PROC_PID
        echo " - Shutdown ...."
    else
        rm -f $PROC_PID
        echo "${PROC_NAME} was not  running."
    fi

}


get_status()
{
    ps ux | grep ${PROC_NAME} | egrep -v "grep|.sh|tail|vim" | awk '{print $2}'
}



stop