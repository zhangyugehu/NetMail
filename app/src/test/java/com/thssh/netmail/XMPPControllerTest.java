package com.thssh.netmail;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/09
 */
public class XMPPControllerTest {
    @Test
    public void init() throws Exception {

        XMPPController controller = XMPPController.getInstance(new XMPPController.IResultDispatcher(){

            @Override
            public void dispatchSuccess(XMPPController.SimpleListener listener, String message) {
                listener.onSuccess(message);
            }

            @Override
            public void dispatchFailure(XMPPController.SimpleListener listener, String error) {
                listener.onFailure(error);
            }
        });
        controller.init(XMPPConfig.newBuilder()
                .setHost("cwindow-im.docmail.cn")
                .setPort(5222)
                .setReconnection(true)
                .setSSLEnable(false)
                .build()
                .config());

        controller.connect(new XMPPController.SimpleListener() {
            @Override
            public void onSuccess(String message) {
                p(message);
            }

            @Override
            public void onFailure(String error) {
                p(error);
            }
        });
    }

    @Test
    public void destroy() throws Exception {

    }

    @Test
    public void getTestInstance() throws Exception {

    }

    @Test
    public void connect() throws Exception {

    }

    @Test
    public void login() throws Exception {

    }

    public void p(String message){

        System.out.println(message);
    }

}