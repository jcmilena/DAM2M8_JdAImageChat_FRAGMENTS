package com.example.jcmilena.jdaimagechat;

public class MultimediaMsg {

    String fromEmail;
    String downloadurl;
    String msg;
    String name;

    public MultimediaMsg(String fromEmail, String downloadurl, String msg, String name) {
        this.fromEmail = fromEmail;
        this.downloadurl = downloadurl;
        this.msg = msg;
        this.name = name;
    }

    public MultimediaMsg() { }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getDownloadurl() {
        return downloadurl;
    }

    public void setDownloadurl(String downloadurl) {
        this.downloadurl = downloadurl;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
