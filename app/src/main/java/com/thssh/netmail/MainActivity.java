package com.thssh.netmail;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;

public class MainActivity extends AppCompatActivity {
    private XMPPController mController;


    private void toast(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mController = XMPPController.getInstance();
    }

    public void addCommand(View view) {
//        login();
        register();
    }

    private void login(){
        mController.login("hutianhang", "a1234567", "test_android", new XMPPController.SimpleListener() {
            @Override
            public void onSuccess(String message) {
                toast("onSuccess " + message);
            }

            @Override
            public void onFailure(String error) {
                toast("onFailure " + error);
            }
        });
    }

    private void register(){

        mController.register(new XMPPController
                .RegisterBuilder()
                .setEmail("hutianhang1@docmail.cn")
                .setName("hutianhang1")
                .setNickName("zhangyugehu")
                .setPassword("a1234567"));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mController.destroy();
//        mController.disConnect(new XMPPController.SimpleListener() {
//            @Override
//            public void onSuccess(String message) {
//                toast("disConnect onSuccess ");
//            }
//
//            @Override
//            public void onFailure(String error) {
//                toast("disConnect onFailure ");
//            }
//        });
    }
}
