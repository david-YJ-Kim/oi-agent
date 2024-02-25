package com.abs.cmn.oia.solace;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.abs.cmn.oia.config.OiaPropertyObject;
import com.abs.cmn.oia.config.SessionConfiguration;
import com.abs.cmn.oia.util.OiaErrorMsgGenerator;
import com.abs.cmn.oia.util.code.OiErrorResponseCode;
import com.abs.cmn.oia.util.code.OiaConstant;
import com.abs.cmn.seq.SequenceManager;
import com.abs.cmn.seq.util.SequenceManageUtil;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SolaceRequester {

    private XMLMessageProducer prod;
    private XMLMessageConsumer cons;
    private Topic topic;

    private String sampleSendTopic;

    private String receiveTopicName; // SVM/DEV/OAG/RPL
    private Queue replyQueue;


    private JCSMPProperties properties;
    private JCSMPSession session;
    OiaPropertyObject propertyObject = OiaPropertyObject.getInstance();

    private SequenceManager sequenceManager;

    private OiaErrorMsgGenerator errorMsgGenerator;

    private String sourceSystem;
    private String siteName;
    private String envType;
    private int requestTimeoutMs;
    private String replyQueueName;
    private String replyTopicName;
    private String testSendTopicName;

    private String seqFilePath;
    private String seqFileName;

    public SolaceRequester(){
        log.info("Initialize Resources");
        try {
            this.initialize();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public boolean initialize() throws IOException {


        this.replyQueueName = this.propertyObject.getReplyQueueName();
        this.replyTopicName = this.convertDestinationName(replyQueueName);
        this.testSendTopicName = this.propertyObject.getTestSendTopicName();
        this.sourceSystem = this.propertyObject.getGroupName();
        this.siteName = this.propertyObject.getSiteName();
        this.envType = this.propertyObject.getEnvType();
        this.requestTimeoutMs = this.propertyObject.getRequestTimeout();

        this.seqFilePath = this.propertyObject.getSequenceFilePath();
        this.seqFileName = this.propertyObject.getSequenceFileName();

        this.sequenceManager = new SequenceManager(
                OiaPropertyObject.getInstance().getGroupName(),
                OiaPropertyObject.getInstance().getSiteName(),
                OiaPropertyObject.getInstance().getEnvType(),
                OiaPropertyObject.getInstance().getSequenceFilePath(),
                OiaPropertyObject.getInstance().getSequenceFileName()
//        		this.sourceSystem, this.siteName, this.envType,
//                                                    this.seqFilePath, this.seqFileName
        );
        this.errorMsgGenerator = new OiaErrorMsgGenerator();
        log.info(this.toString());

        try {
            this.properties = SessionConfiguration.getSessionConfiguration().getProperty(String.valueOf(System.currentTimeMillis()));
            this.session = JCSMPFactory.onlyInstance().createSession(this.properties);
            this.session.connect();
            log.info("Session has been started. sessionName: {}, sessionStats: {}"
                    , this.session.getSessionName(), this.session.getSessionStats().toString());


            /*
             * change requester to publisher
             **/
            this.replyQueue = JCSMPFactory.onlyInstance().createQueue(this.replyQueueName);
            this.prod = session.getMessageProducer(new PrintingPubCallback());
            this.cons = session.getMessageConsumer((XMLMessageListener)null);


            cons.start();

            return true;
        } catch(Exception e)
        {
            log.error("Sender.initialize() Exception # ",e);

            return false;
        }
    }

    public String requestReply(String corRelationId, String target, String payload, String cid) throws Exception {

        //Text Messsage
        TextMessage message = this.generateMessage(payload, cid, corRelationId, this.replyTopicName);


        //TODO Sequecene Library 진입 점 sequenceManager.getTargetName(target, cid, payload)
        String topicName = sequenceManager.getTargetName(target, cid, payload);
        //this.testSendTopicName;
        Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);

        String response = null;
        try {
            log.info("[ID:{}] OIA send request to Solace Its topic: {}", corRelationId, topicName);

            message.setCorrelationId(corRelationId);

            prod.send(message, topic);
            log.info("Message has been sent to destination: {}.", topicName);

            response = this.runSelectReceiver(corRelationId, this.replyQueue);
            return response;

        } catch(Exception e) {
            e.printStackTrace();
            log.error("[ID:{}] request Reply Exception : {}. message:{} ",corRelationId ,e, e.getMessage());
            throw new Exception(e.getMessage());

        }

    }

    private TextMessage generateMessage(String payload, String cid, String selectKey, String replyTopicName) throws SDTException {
        TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        message.setProperties(this.generateCustomProperty(cid, selectKey, replyTopicName));
        message.setText(payload);

        message.setDeliveryMode(DeliveryMode.PERSISTENT);
        return message;
    }

    private SDTMap generateCustomProperty(String cid, String selectKey, String replyTopicName) throws SDTException {

        SDTMap map = JCSMPFactory.onlyInstance().createMap();
        map.putString(OiaConstant.cid.name(), cid);
        map.putString(OiaConstant.messageId.name(), SequenceManageUtil.generateMessageID());
        map.putString(OiaConstant.selectorKey.name(), selectKey);
        map.putString(OiaConstant.replyTopicName.name(), replyTopicName);
        return map;
    }

    private String generateErrorMessage(){
        return String.format(OiErrorResponseCode.errorResponseFormat, OiaConstant.Fail.name(), "TimeOut");
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

    private String convertDestinationName(String name){

        if(name.contains("_")){
            log.info("Queue name will be return into Topic Name. inputName:{},  isQueueName:{}",
                    name, name.contains("_"));
            return name.replace('_', '/');

        }else if(name.contains("/")){
            log.info("Topic name will be return into Queue Name. inputName:{},  isTopicName:{}",
                    name, name.contains("/"));
            return name.replace('/', '_');

        }else{
            log.error("inputName:{},  isTopicName:{}, isQueueName:{}",
                    name, name.contains("_"), name.contains("/"));
            return name;

        }
    }


    private String runSelectReceiver(String selectKey, Queue queue) throws Exception {

        if(this.prod == null){
            this.initialize();
        }

        String response = null;

        // Set Consumer Property
        ConsumerFlowProperties flowProps = new ConsumerFlowProperties();
        flowProps.setEndpoint(queue);
        flowProps.setSelector(String.format("%s = '%s'", OiaConstant.selectorKey.name(), selectKey));

        FlowReceiver flow = session.createFlow(null, flowProps);
        flow.start();
        log.info("Flow has been opened.");

        /*
         * using request and reply
         **/
        log.info("[ID:{}] OIA start wait to receive reply from Solace ", selectKey);

        BytesXMLMessage reply = flow.receive(this.requestTimeoutMs);

        SDTMap customMap = reply.getProperties();

        String repliedSelectKey = customMap.getString(OiaConstant.selectorKey.name());
        String retCode = customMap.getString(OiaConstant.retCode.name());

        try{
            if(reply != null){

                if(reply instanceof TextMessage){

                    if(selectKey.equals(repliedSelectKey)){

                        response = ((TextMessage) reply).getText();
                        log.info("[ID:{}] Reply Completed", selectKey);
                        // 정상 처리 but Biz Fail
                        boolean isSuccess = retCode.equals(OiaConstant.Success.name()) ? true : false;
                        if(!isSuccess){
                            String errorMsg = this.errorMsgGenerator.generateErrorMessage("MES Exception", response);
                            log.info("[ID:{}] Reply Completed. But Biz Error. replyPayload: {}, errorMsg: {}", selectKey, response, errorMsg);
                            throw new Exception(errorMsg);
                        }

                    }else {

                        String exceptionMessage = String.format("SelectKey is not matched. ourKey: %s. replyKey: %s", selectKey, repliedSelectKey);
                        String errorMsg = this.errorMsgGenerator.generateErrorMessage("OIA Exception", "OIA_KEY_INVALID", exceptionMessage);
                        log.error("[ID:{}] Corelation ID가 일치 하지 않는 경우라면.... 솔라스의 Req-Rep 기능의 이슈.. 라고 판단 가능...replyPayload: {}, errorMsg: {}", selectKey, response, errorMsg);
                        throw new Exception(errorMsg);

                    }

                }else {
                    // TODO TextMessage 객체 이외 대응
                    String exceptionMessage = String.format("[SelectKey: %s]  Text 메시지 객체가 아닌 경우, 현재 시점에서 대응 안함. 추후 대응 필요", selectKey);
                    log.error("[ID:{}] Text 메시지 객체가 아닌 경우, 현재 시점에서 대응 안함. 추후 대응 필요", selectKey);
                    String errorMsg = this.errorMsgGenerator.generateErrorMessage("OIA Exception", "OIA_RECV_NON_TEXT", exceptionMessage);
                    throw new Exception(errorMsg);
                }

            }else{
                // REPLY가 Null 인 경우
                String exceptionMessage = String.format("[SelectKey: %s]  Reply가 Null...", selectKey);
                log.error("[ID:{}] REPLY가 NULL 인 경우 ", selectKey);
                String errorMsg = this.errorMsgGenerator.generateErrorMessage("OIA Exception", "OIA_REPLY_NULL", exceptionMessage);
                throw new Exception(errorMsg);

            }
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e.getMessage());

        }finally {
            flow.close();
            log.info("[ID:{}] Flow has been closed.", selectKey);
        }

        return response;


    }



    @Override
    public String toString() {
        return "SolaceRequester{" +
//                "prod=" + prod +
//                ", cons=" + cons +
//                ", topic=" + topic +
//                ", sampleSendTopic='" + sampleSendTopic + '\'' +
//                ", receiveTopicName='" + receiveTopicName + '\'' +
//                ", replyQueue=" + replyQueue +
//                ", properties=" + properties +
//                ", session=" + session +
//                ", propertyObject=" + propertyObject +
                ", sequenceManager=" + sequenceManager +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", siteName='" + siteName + '\'' +
                ", envType='" + envType + '\'' +
                ", requestTimeoutMs=" + requestTimeoutMs +
                ", replyQueueName='" + replyQueueName + '\'' +
                ", replyTopicName='" + replyTopicName + '\'' +
                ", testSendTopicName='" + testSendTopicName + '\'' +
                ", seqFilePath='" + seqFilePath + '\'' +
                ", seqFileName='" + seqFileName + '\'' +
                '}';
    }
}
