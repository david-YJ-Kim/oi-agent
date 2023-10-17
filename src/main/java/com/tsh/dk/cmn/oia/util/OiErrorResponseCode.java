package com.abs.cmn.oia.util;

import org.springframework.beans.factory.annotation.Value;

public final class OiErrorResponseCode {

    public static final String errorResponseFormat = "{\"retCode\":%s,\"retMessage\":%s}";

    public static void main(String[] args) {
        System.out.println(
                String.format(errorResponseFormat, "A", "B")
        );
    }
}