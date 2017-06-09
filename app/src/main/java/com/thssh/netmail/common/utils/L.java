package com.thssh.netmail.common.utils;

import android.util.Log;

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/09
 */

public class L {

    private static final boolean DEBUG_FLAG = true;
    private static final String DEF_TAG = "app_netmail_log";

    public static void v(String tag, String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.v(tag, message);
    }
    public static void v(String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.v(DEF_TAG, message);
    }

    public static void d(String tag, String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.d(tag, message);
    }
    public static void d(String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.d(DEF_TAG, message);
    }

    public static void i(String tag, String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.i(tag, message);
    }
    public static void i(String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.i(DEF_TAG, message);
    }

    public static void w(String tag, String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.w(tag, message);
    }
    public static void w(String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.e(DEF_TAG, message);
    }

    public static void e(String tag, String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.e(tag, message);
    }
    public static void e(String message){
        if(!DEBUG_FLAG){ return; }
        message = getStackTraceMessage(message);
        Log.e(DEF_TAG, message);
    }



    private static String getStackTraceMessage(String message){
        String trace = getStackTrace();
        return trace == null ? message : trace + " - " + message;
    }

    private static String getStackTrace() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        if (sts == null) {
            return null;
        }

        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }
            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }
            if (st.getClassName().contains(L.class.getName())) {
                continue;
            }
            return "[" + Thread.currentThread().getId() + ": "
                    + st.getFileName() + ":" + st.getLineNumber() + "]";

        }
        return null;
    }
}
