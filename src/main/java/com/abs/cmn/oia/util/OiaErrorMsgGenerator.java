package com.abs.cmn.oia.util;

import com.abs.cmn.oia.util.code.OiaConstant;
import org.json.JSONObject;

public class OiaErrorMsgGenerator {

    public static void main(String[] args) {
        System.out.println(
                new OiaErrorMsgGenerator().generateErrorMessage("MES Exception", "{\"retCode\":\"BRS_ERR_LOT_001\",\"retMessage\":\"[Lot Hold YN : Y]\",\"lotId\":null,\"carrId\":null,\"eqpId\":null}")
        );
    }


    public String generateErrorMessage(String exceptionType, String replyMessage){
        JSONObject replObj = new JSONObject(replyMessage);
        String exceptionMessage = replObj.getString(OiaConstant.retMessage.name());
        String exceptionId = replObj.getString(OiaConstant.retCode.name());

        return String.format(OiaErrorMsgGenerator.errorFormat, exceptionType, exceptionMessage, exceptionId);
    }

    public String generateErrorMessage(String exceptionType, String exceptionId, String exceptionMessage){

        return String.format(OiaErrorMsgGenerator.errorFormat, exceptionType, exceptionMessage, exceptionId);
    }

    public static String errorFormat = "{\n" +
            "  \"exceptionType\": \"%s\",\n" +
            "    \"exceptionMessage\": \"%s\",\n" +
            "    \"uiExceptionMsgEnable\": false,\n" +
            "    \"uiExceptionId\":  \"%s\",\n" +
            "    \"uiExceptionParams\": null,\n" +
            "    \"exceptionClass\": [\n" +
            "    ],\n" +
            "    \"redirectUrl\": \"\"\n" +
            "}";

}
