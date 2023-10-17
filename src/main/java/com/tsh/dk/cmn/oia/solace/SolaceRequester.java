package com.abs.cmn.oia.solace;

import com.abs.cmn.oia.SolaceConfiguration;
import com.abs.cmn.oia.util.OiaCommonCode;
import com.abs.cmn.oia.util.SequenceManageUtil;
import com.solacesystems.jcsmp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
public class SolaceRequester {

    private JCSMPProperties properties = new JCSMPProperties();
    private JCSMPSession sessionP;
    private XMLMessageProducer prod;
    private XMLMessageConsumer cons;
    private FlowReceiver flow;
    private ConsumerFlowProperties flowProps;

    //    private Receiver rcv;
    private Topic topic;

    //PubCallback Event class
    private int requestorTimeout = 10000;

    @Value("${oia.reply.queue.name}")
    private String queue_name;
    private Queue replyQueue;

    // 임시 변수
    private String msg_id;
    private String eqp_id;

    private String errResp = "{\"retCode\":\"Fail\",\"retMessage\":\"Time Out~~~~~\"}";

    //    private JCSMPProperties getProperty(String postfixClientName){
//        log.info(this.toString());
//
//        JCSMPProperties properties = new JCSMPProperties();
//
//        properties.setProperty(JCSMPProperties.HOST, "52.79.64.176:55555");
//        //solace msgVpn명
//        properties.setProperty(JCSMPProperties.VPN_NAME, "MES");
//        //solace msgVpn에 접속할 클라이언트사용자명
//        properties.setProperty(JCSMPProperties.USERNAME, "default");
//        //solace msgVpn에 접속할 클라이언트사용자 패스워드(생략 가능)
//        properties.setProperty(JCSMPProperties.PASSWORD, "admin");
//        //Allication client name 설정 - 동일 msgVpn 내에서 uniq 해야 함
//        properties.setProperty(JCSMPProperties.CLIENT_NAME, "OIA" + postfixClientName);
//        //endpoint에 등록되어 있는 subscription으로 인해 발생하는 에러 무시
//        properties.setProperty(JCSMPProperties.IGNORE_DUPLICATE_SUBSCRIPTION_ERROR, true);
//
//        JCSMPChannelProperties chProp = new JCSMPChannelProperties();
//        chProp.setReconnectRetries(5); // 세션 다운 시 재 연결 트라이 횟수
//        chProp.setConnectRetriesPerHost(5); // 세션 리트라이 간격
//
//        properties.setProperty(JCSMPChannelProperties.RECONNECT_RETRIES, chProp);
//
//        return properties;
//    }
    public boolean initialize() {
        try {
            log.info("## test info : "+SolaceConfiguration.getSessionConfiguration().getClientName());

            properties = SolaceConfiguration.getSessionConfiguration().getProperty(OiaCommonCode.REQUESTER.name());
//            properties = this.getProperty("REQ");
            //SpringJCSMPFactory를 이용한 JCSMPSession 생성(JCSMPFactory 사용하는 것과 동일
            // -> session = JCSMPFactory.onlyInstance().createSession(properties);)
            sessionP = JCSMPFactory.onlyInstance().createSession(properties);
            //session 연결 - Application 별로 최소 연결 권장(쓰레드를 사용할 경우 공유 사용 권장)
            sessionP.connect();

            /*
             * change requester to publisher
             **/
            this.replyQueue = JCSMPFactory.onlyInstance().createQueue("SVM_DEV_OAG_RPL");
            this.flowProps = new ConsumerFlowProperties();
            flowProps.setEndpoint(replyQueue);
            this.flow = sessionP.createFlow(null, flowProps);
            this.prod = sessionP.getMessageProducer(new PrintingPubCallback());
            this.cons = sessionP.getMessageConsumer((XMLMessageListener)null);


            cons.start();
            flow.start();

            return true;
        } catch(Exception e)
        {
            log.error("Sender.initialize() Exception # ",e);

            return false;
        }
    }



    public String requestReply(String msg) {
        try {

            log.info("2. in sendMessage - msg : "+msg);


            String corRelationId = SequenceManageUtil.generateMessageID();
            log.info("2-1. in sendMessage - corRelationId : "+corRelationId);

            //Text Messsage
            TextMessage jcsmpMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);

            //Custom Property
            SDTMap map = JCSMPFactory.onlyInstance().createMap();

            // 테스트 topic 획득
            topic = JCSMPFactory.onlyInstance().createTopic("SVM/DEV/BRS/LOT/00");//SequenceManager.getTargetName(msg) );
            // 운영시 사용 코드
//            topic = JCSMPFactory.onlyInstance().createTopic( SequenceManager.getTargetName("targetSystem", "cid", "obj.toString()", "ownerSystem" ) );


            eqp_id = "EQP-0001";			// 임의설정

            //Custom Property 설정
            map.putString("cid", "BRS_CARR_STATE_CHANGE_CLEAN");
            map.putString("messageId", SequenceManageUtil.generateMessageID());

            jcsmpMsg.setProperties(map);
            jcsmpMsg.setText(msg);
            //Application messageId 설정
            jcsmpMsg.setCorrelationId(corRelationId);
            jcsmpMsg.setReplyTo(replyQueue);
            jcsmpMsg.setDeliveryMode(DeliveryMode.PERSISTENT);

            log.info("message sending.");

            //Multi Application이 동일한 큐에 매핑된 Multi Topic에 송신한다고 가정한 코드
            prod.send(jcsmpMsg, topic);

            log.info("3. Request before");

            /*
             * using request and reply
             **/

            BytesXMLMessage reply = flow.receive(requestorTimeout);

            if ( reply != null && reply instanceof TextMessage ) {
                log.info("4. after reply , msg"+reply.dump());

                reply.ackMessage();
                return ((TextMessage) reply).getText();
            } else {
                log.info("4. no reply msg");

                if (reply.getCorrelationId().equals(jcsmpMsg.getCorrelationId())) {
                    cons.close();
                    return new String(reply.getBytes().toString());
                }
            }
            return errResp;

        } catch(Exception e) {
            log.error("Sender.sendMessage() Exception # ",e);
//			if (session != null && !session.isClosed()) try { session.closeSession(); } catch (Exception e1) {}
        }

        return errResp;
    }

    public class PrintingPubCallback implements JCSMPStreamingPublishCorrelatingEventHandler {

        @Override
        public void handleErrorEx(Object messageID, JCSMPException cause, long timestamp) {
            System.err.println("Error occurred for message: " + (String)messageID);
            cause.printStackTrace();
        }

        @Override
        public void responseReceivedEx(Object messageID) {
            System.out.println("Response received for message: " + (String)messageID);
        }
    }
}
