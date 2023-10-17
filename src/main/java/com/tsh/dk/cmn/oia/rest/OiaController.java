package com.abs.cmn.oia.rest;

//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.web.PagedResourcesAssembler;
import com.abs.cmn.oia.solace.SolaceRequester;
import com.solacesystems.jcsmp.JCSMPException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@CrossOrigin
public class OiaController implements ApplicationListener<ApplicationStartedEvent>{

    private SolaceRequester solaceRequester;
    //    private static SolaceRequester solaceRequester = new SolaceRequester();
//    static {
//        solaceRequester.initialize();
//    }
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {

        this.solaceRequester = new SolaceRequester();
        this.solaceRequester.initialize();
    }

    @RequestMapping(value = "/**", method = RequestMethod.POST)
    public ResponseEntity<?> requestSend(@RequestBody String payload) {

        // TODO Payload가 Null 인지 확인 안한다.

        log.info(payload);
        String reply;
        HttpStatus status;

        try{
            reply = this.solaceRequester.requestReply(payload);
            status = HttpStatus.OK;

        }catch (Exception e){
            e.printStackTrace();
            reply = this.setErrorResponse(payload);
            status = HttpStatus.BAD_REQUEST;
        }

        log.info(reply, status.toString());
        return ResponseEntity.status(status).body(reply);
    }

    private String setErrorResponse(String payload){
        return "";
    }
}
