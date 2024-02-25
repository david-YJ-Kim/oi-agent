#!/bin/bash
# 프로세스 명을 명시한다.
readonly PROC_NAME="mes-pos-product"
# jar 파일
readonly DAEMON="../target/mes-pos-1.0.0-SNAPSHOT-exec.jar"
# 프로세스 아이디가 존재할 패스를 설정
readonly PID_PATH="./"
readonly PROC_PID="${PID_PATH}${PROC_NAME}.pid"

# 시작 함수
start()
{
    echo "Starting  ${PROC_NAME}..."
    local PID=$(get_status)
    if [ -n "${PID}" ]; then
        echo "${PROC_NAME} is already running"
    else
        nohup java -jar -Dspring.profiles.active=product -Dname=${PROC_NAME} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./ -Xms1024m -Xmx2048m -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m -XX:+UseG1GC "${DAEMON}" > /dev/null 2>&1 &
        local PID=${!}

        if [ -n ${PID} ]; then
            echo " - Starting..."
            echo " - Created Process ID in ${PROC_PID}"
            echo ${PID} > ${PROC_PID}
        else
            echo " - failed to start."
        fi
    fi
}

# 중지
stop()
{
    echo "Stopping ${PROC_NAME}..."
    local DAEMON_PID=`cat "${PROC_PID}"`
    local PID=$(get_status)
    echo "DAEMON_PID=${DAEMON_PID} : PID=${PID}"

    if [ -n "${PID}" ]; then
        curl -X POST http://localhost:11021/actuator/shutdown
        kill -15 $PID
        rm -f $PROC_PID
        echo " - Shutdown ...."
    else
        rm -f $PROC_PID
        echo "${PROC_NAME} was not  running."
    fi

}
# 상태
status()
{
    local PID=$(get_status)
    echo "PID = ${PID}"
    if [ -n "${PID}" ]; then
        echo "${PROC_NAME} is running"
    else
        echo "${PROC_NAME} is stopped"

#       emma_check=`ps -ef | grep -v "grep" | grep "$PROC_NAME" | wc -l`
 #       echo "$emma_check":
#       if [ "$emma_check" == "0"  ]; then
#               echo "${PROC_NAME} is stopped"
#       else
#               echo "${PROC_NAME} is running"

        # start daemon
        #nohup java -jar "${DAEMON}" > /dev/null 2>&1 &
    fi
    curl http://localhost:11021/actuator/health
    echo ""
}

restart()
{
    stop

    sleep 10

    local SET=$(seq 0 30)
    for i in $SET
    do
       echo "Running loop seq : " $i

       local PID=$(get_status)
       if [ -n "${PID}" ]; then
          echo "${PROC_NAME} is already running"
       else
          start
          break
       fi
       sleep 5
    done
}



get_status()
{
    ps ux | grep ${PROC_NAME} | egrep -v "grep|.sh|tail|vim" | awk '{print $2}'
}

# 케이스 별로 함수를 호출하도록 한다.

case "$1" in
    start)
        start
        sleep 7
        ;;
    stop)
        stop
        sleep 5
        ;;
    restart)
       restart
       sleep 7
       ;;
    status)
    status "${PROC_NAME}"
    ;;
    *)
    echo "Usage: $0 {start | stop | status }"
esac
exit 0
