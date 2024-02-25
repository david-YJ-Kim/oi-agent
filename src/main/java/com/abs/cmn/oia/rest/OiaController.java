package com.abs.cmn.oia.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.abs.cmn.oia.solace.SolaceRequester;
import com.abs.cmn.oia.util.SequenceManageUtil;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin
@Slf4j
//public class OiaController implements ApplicationListener<ApplicationStartedEvent>{
public class OiaController {

    @Autowired
    private SolaceRequester solaceRequester;

//    @Override
//    public void onApplicationEvent(ApplicationStartedEvent event) {
//
//
//        this.solaceRequester = new SolaceRequester();
//        try {
//            this.solaceRequester.initialize();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @RequestMapping(value = "/**", method = RequestMethod.POST)
    public ResponseEntity<?> requestSend(HttpServletRequest request) throws IOException {



        // TODO Payload가 Null 인지 확인 안한다.
        String corRelationId = SequenceManageUtil.generateMessageID();
        String[] urlParts = request.getRequestURI().split("/");
        String cid = urlParts[urlParts.length -1];
        String target = cid.split("_")[0];
        String payload = IOUtils.toString(request.getInputStream());


        log.info("[ID:{}] OIA receive message. its' uri:{} cid: {}, target: {}, payload: {}",
                corRelationId, request.getRequestURI(), cid, target, payload);
        String reply;
        HttpStatus status;

        try{
            reply = this.solaceRequester.requestReply(corRelationId, target,  payload, cid);
            status = HttpStatus.OK;

        }catch (Exception e){
            e.printStackTrace();
            reply = e.getMessage();
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("[ID:{}] Error has been occurred. it's error contents:{}", corRelationId, e.toString());
        }

        log.info("[ID:{}] OIA reply message. it's reply payload: {}, and it's status:{}", corRelationId, reply, status.toString());
        return ResponseEntity.status(status).body(reply);
    }

}
