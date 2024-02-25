#!/bin/bash
# Set Process profile
profile=$1

# Declare process name.
readonly PROC_NAME="mes-oia-$profile"
# jar Files
readonly DAEMON="../target/oia-0.0.1-SNAPSHOT.jar"
# Set up where to place Process ID
readonly PID_PATH="./"
readonly PROC_PID="${PID_PATH}${PROC_NAME}.pid"

start()
{
  echo "nohup java -jar -Dspring.profiles.active=$profile -Dname=${PROC_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./ -Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m -XX:+UseG1GC "${DAEMON}" > /dev/null 2>&1 &"
  nohup java -jar -Dspring.profiles.active=$profile -Dname=${PROC_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./ -Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m -XX:+UseG1GC "${DAEMON}" > /dev/null 2>&1 &
  local PID=$(get_status)
  echo " - Starting..."
  echo " - Created Process ID in ${PROC_PID}"
  echo ${PID} > ${PROC_PID}
}


get_status()
{
    ps ux | grep ${PROC_NAME} | egrep -v "grep|.sh|tail|vim" | awk '{print $2}'
}


start