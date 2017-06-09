package com.thssh.netmail;

import android.os.Build;

import org.jivesoftware.smack.ConnectionConfiguration;

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/06
 */

public class XMPPConfig {

    private Builder builder;

    private XMPPConfig(Builder builder) {
        this.builder = builder;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public ConnectionConfiguration config(){
        ConnectionConfiguration config = new ConnectionConfiguration(builder.host, builder.port);
        if (Build.VERSION.SDK_INT >= 14) {
            config.setTruststoreType("AndroidCAStore"); //$NON-NLS-1$
            config.setTruststorePassword(null);
            config.setTruststorePath(null);
        } else {
            config.setTruststoreType("BKS"); //$NON-NLS-1$
            String path = System.getProperty("javax.net.ssl.trustStore"); //$NON-NLS-1$
            if (path == null)
                path = System.getProperty("java.home") + java.io.File.separator //$NON-NLS-1$
                        + "etc" + java.io.File.separator + "security" //$NON-NLS-1$ //$NON-NLS-2$
                        + java.io.File.separator + "cacerts.bks"; //$NON-NLS-1$
            config.setTruststorePath(path);
        }
        config.setSASLAuthenticationEnabled(builder.isSSLEnable);
        config.setReconnectionAllowed(builder.isReconnection);
        config.setCompressionEnabled(builder.isCompression);
        config.setSecurityMode(builder.securityMode); // SecurityMode.required/disabled
        return config;
    }

    /**
     * 参数构造器
     */
    public static class Builder{
        private String host;
        private int port;
        private boolean isSSLEnable;
        private boolean isReconnection;
        private boolean isCompression;
        private ConnectionConfiguration.SecurityMode securityMode;

        public Builder() {
            this.port = 5222;
            this.securityMode = ConnectionConfiguration.SecurityMode.disabled;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setSSLEnable(boolean SSLEnable) {
            isSSLEnable = SSLEnable;
            return this;
        }

        public Builder setReconnection(boolean reconnection) {
            isReconnection = reconnection;
            return this;
        }

        public Builder setCompression(boolean compression) {
            isCompression = compression;
            return this;
        }

        public Builder setSecurityMode(ConnectionConfiguration.SecurityMode securityMode) {
            this.securityMode = securityMode;
            return this;
        }

        public XMPPConfig build(){
            return new XMPPConfig(this);
        }
    }
}
