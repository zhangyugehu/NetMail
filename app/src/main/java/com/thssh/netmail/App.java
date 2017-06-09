package com.thssh.netmail;

import android.app.Application;
import android.util.Log;

import com.thssh.netmail.common.utils.L;
import com.thssh.netmail.contrant.XMPPContant;

import org.jivesoftware.smack.XMPPException;

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/09
 */

public class App extends Application {

    private XMPPController xmppController;

    @Override
    public void onCreate() {
        super.onCreate();
        initXMPP();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d("App", "onTerminate: ");
    }

    private void initXMPP() {
        try {
            xmppController = XMPPController.getInstance();
            xmppController.init(XMPPConfig.newBuilder()
                    .setHost(XMPPContant.XMPP_HOST)
                    .setPort(XMPPContant.XMPP_PORT)
                    .setReconnection(true)
                    .setSSLEnable(false)
                    .build());
            xmppController.connect(new XMPPController.SimpleListener() {
                @Override
                public void onSuccess(String message) {
                    L.i("connect im server. " + message);
                }

                @Override
                public void onFailure(String error) {
                    L.e("connect im server error. "+ error);
                }
            });
        } catch (XMPPException e) {
            L.e("init xmpp exception: " + e.toString());
        }
    }
}
