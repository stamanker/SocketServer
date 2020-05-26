package com.stamanker.socket;

import java.util.Date;

public abstract class Response {

    private String type;
    private byte[] result;
    private int code;
    private String msg;

    public Response(int code, String type, String msg) {
        this.code = code;
        this.type = type;
        this.msg = (msg == null ? "" : msg);
    }

    public byte[] getResult() {
        if (result == null) {
            result = composeResult();
        }
        return result;
    }

    protected abstract byte[] composeResult();

    public String getHeaders() {
        String response = "HTTP/1.1 " + code + " " + msg + "\n" +
                "Date: " + new Date() + "\n" +
                "Server: StamankerServer 0.1\n" +
                "Last-Modified: " + new Date() + "\n" +
                "Content-Length: " + getResult().length + "\n" +
                "Content-Type: " + type + "\n";
//        if (type.equals("text/html")) {
        response += "Connection: Close\n\n";
//        }
        return response;
    }

}
