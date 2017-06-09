package com.thssh.netmail;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.thssh.netmail.common.utils.L;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.Version;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/06
 */

public class XMPPController implements Runnable{

    public interface PacketListener{
        void onSuccess(PacketCollector collector);
        void onFailure(String error);
    }
    public interface SimpleListener{
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface IResultDispatcher{
        void dispatchSuccess(SimpleListener listener, String message);
        void dispatchFailure(SimpleListener listener, String error);
    }

    static final class MainThreadDispatcher implements IResultDispatcher{

        private Handler handler;

        public MainThreadDispatcher() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void dispatchSuccess(final SimpleListener listener, final String message) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(listener != null){ listener.onSuccess(message);}
                }
            });
        }

        @Override
        public void dispatchFailure(final SimpleListener listener, final String error) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if(listener != null){ listener.onFailure(error);}
                }
            });
        }
    }

    public static class RegisterBuilder{
        private static final String EMAIL = "email";
        private static final String NICK_NAME = "name";

        private String name;
        private String password;
        private String nickName;
        private String email;

        public RegisterBuilder() {
        }

        public RegisterBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public RegisterBuilder setEmail(String email) {
            this.email = email;
            return this;
        }

        public RegisterBuilder setNickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public RegisterBuilder setPassword(String password) {
            this.password = password;
            return this;
        }

        public String check() {
            if(TextUtils.isEmpty(name)){
                return "用户名不能为空。";
            }
            if(TextUtils.isEmpty(password)){
                return "密码不能为空。";
            }
            return null;
        }

        public Registration build(XMPPConnection conn) {
            Registration registration = new Registration();
            registration.setType(IQ.Type.GET);
            registration.setTo(conn.getServiceName());
            // 注意这里createAccount注册时，参数是UserName，不是jid，是"@"前面的部分。
            registration.setUsername(name);
            registration.setPassword(password);
            // 这边addAttribute不能为空，否则出错。所以做个标志是android手机创建的吧！！！！！
            registration.addAttribute(EMAIL, email);
            registration.addAttribute(NICK_NAME, nickName);
            registration.addAttribute("android", "geolo_createUser_android");
            return registration;
        }
    }

    /**
     * XMPPConnection的Package发送任务
     */
    public static class Command {

        public String des;
        public Packet packet;
        public PacketFilter filter;
        public PacketListener listener;

    }

    private static XMPPController instance;
    private IResultDispatcher mDispatcher;
    private boolean mInterreput;
    private XMPPConnection mConnection;
    private XMPPConfig mConfig;

    private ExecutorService mCommandExecutors;
    private ExecutorService mExecutors;
    private Command mCommand;
    private List<Command> mTasks;

    private XMPPController(IResultDispatcher dispatcher) {
        mDispatcher = dispatcher;
        mCommandExecutors = Executors.newSingleThreadExecutor();
        mExecutors = Executors.newSingleThreadExecutor();
        mTasks = new ArrayList<>();
        mCommandExecutors.submit(this);
    }

    private void configureConnection(ProviderManager pm) {
        // Private Data Storage
        pm.addIQProvider("query", "jabber:iq:private",new PrivateDataManager.PrivateDataIQProvider());
        // Time
        try {
            pm.addIQProvider("query", "jabber:iq:time",Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Roster Exchange
        pm.addExtensionProvider("x", "jabber:x:roster",new RosterExchangeProvider());
        // Message Events
        pm.addExtensionProvider("x", "jabber:x:event",new MessageEventProvider());
        // Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates",new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates",new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates",new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates",new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates",new ChatStateExtension.Provider());
        // XHTML
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",new XHTMLExtensionProvider());
        // Group Chat Invitations
        pm.addExtensionProvider("x", "jabber:x:conference",new GroupChatInvitation.Provider());
        // Service Discovery # Items //解析房间列表
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",new DiscoverItemsProvider());
        // Service Discovery # Info //某一个房间的信息
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",new DiscoverInfoProvider());
        // Data Forms
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        // MUC User
        pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",new MUCUserProvider());
        // MUC Admin
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",new MUCAdminProvider());
        // MUC Owner
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",new MUCOwnerProvider());
        // Delayed Delivery
        pm.addExtensionProvider("x", "jabber:x:delay",new DelayInformationProvider());
//		pm.addIQProvider(arg0, arg1, arg2);
        // Version
        pm.addIQProvider("query", "jabber:iq:version", Version.class);
        // VCard
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        // Offline Message Requests
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline",new OfflineMessageRequest.Provider());
        // Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline",new OfflineMessageInfo.Provider());
        // Last Activity
        pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
        // User Search
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        // SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup",new SharedGroupsInfo.Provider());
        pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        // JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address",new MultipleAddressesProvider());
        pm.addIQProvider("si", "http://jabber.org/protocol/si",new StreamInitiationProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",new BytestreamsProvider());
        pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands",new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action","http://jabber.org/protocol/commands",new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale","http://jabber.org/protocol/commands",new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload","http://jabber.org/protocol/commands",new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid","http://jabber.org/protocol/commands",new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired","http://jabber.org/protocol/commands",new AdHocCommandDataProvider.SessionExpiredError());


//        pm.addIQProvider("ack", "x-AckMessage", new SendShakeHand()) ;
//        pm.addIQProvider("chat", "urn:xmpp:archive", new ChatIQProvider());
//        pm.addIQProvider("list", "urn:xmpp:archive", new ListIQProvider());
//        pm.addIQProvider("query", "http://jabber.org/protocol/muc#nicknames",new GroupNickProvider());
//        pm.addIQProvider("query", "http://jabber.org/protocol/x-HisMessage", new ListMsgIQProvider1()) ;
    }

    private Future<?> putBackground(Runnable runnable){
        return mExecutors.submit(runnable);
    }

    private void publishSimpleSuccess(SimpleListener listener, String message){
        if(mDispatcher != null) mDispatcher.dispatchSuccess(listener, message);
    }

    private void publishSimpleFailure(SimpleListener listener, String error){
        if(mDispatcher != null) mDispatcher.dispatchFailure(listener, error);
    }

    @Override
    public void run() {
        for(;;){
            if(mInterreput){ break; }
            if(mCommand == null){ continue; }
            try {
                L.d("require: " + mCommand.packet.toXML());
                mConnection.sendPacket(mCommand.packet);
                if(mCommand.filter == null){
                    mCommand.filter = new PacketIDFilter(mCommand.packet.getPacketID());
                }
                PacketCollector collector = mConnection.createPacketCollector(mCommand.filter);
                // TODO debug使用
//                Packet packet = null;
//                while ((packet = collector.nextResult()) != null){
//                    L.d("response: " + packet.toXML());
//                }
                if(mCommand.listener != null){ mCommand.listener.onSuccess(collector);}
            }catch (Exception e){
                if(mCommand.listener != null){ mCommand.listener.onFailure(e.getMessage()); }
            }
            mTasks.remove(mCommand);
            int size = mTasks.size();
            if(size > 0){
                mCommand = mTasks.get(0);
            }else{
                mCommand = null;
            }
        }
    }

    public void init(XMPPConfig config) throws XMPPException {
        if(mConnection != null){
            throw new XMPPException("XMPPConnection already inited.");
        }
        mConfig = config;
        mConnection = new XMPPConnection(config.config());
        configureConnection(ProviderManager.getInstance());
    }

    public void destroy(){
        L.d("XMPPController", "destroy ");
        mTasks.clear();
        this.mInterreput = true;
    }

    public static XMPPController getInstance(){
        return getInstance(new MainThreadDispatcher());
    }

    public static XMPPController getInstance(IResultDispatcher dispatcher){
        synchronized (XMPPController.class){
            if(instance == null){
                synchronized (XMPPController.class){
                    if(instance == null){
                        instance = new XMPPController(dispatcher);
                    }
                }
            }
            return instance;
        }
    }

    private void enqueue(Command command){
        if(mCommand == null){
            mCommand = command;
        }
        mTasks.add(command);
    }

    /**
     * 与服务器建立连接
     * @param listener
     */
    public void connect(final SimpleListener listener) {
        putBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mConnection != null) {
                        mConnection.disconnect();
                        mConnection = null;
                    }
                    init(mConfig);
                    mConnection.connect();
                    publishSimpleSuccess(listener, "connected!");
                }catch (Exception e){
                    publishSimpleFailure(listener, e.toString());
                }

            }
        });
    }

    /**
     * 断开与服务器连接
     * @param listener
     */
    public void disConnect(final SimpleListener listener){
        putBackground(new Runnable() {
            @Override
            public void run() {
                try{
                    mConnection.disconnect();
                    mConnection = null;
                    publishSimpleSuccess(listener, ("disconnected!"));
                }catch (Exception e){
                    publishSimpleFailure(listener, e.toString());
                }
            }
        });
    }

    /**
     * 注册用户
     * @param builder
     */
    public void register(RegisterBuilder builder){
        String checkRst = builder.check();
        if(checkRst != null){
            // 参数错误
            return;
        }
        Registration registration = builder.build(mConnection);
//        L.d(registration.toXML());
        // 利用包过滤器，PacketFilter是一个接口，这些过滤器可以逻辑地组合对于更复杂的信息包过滤通过使用AndFilter和OrFilter过滤器。
        // 也可以通过实现这个接口定义您自己的过滤器,过滤数据包与特定的ID(实际代码应该使用PacketIDFilter代替)。
        // 参数：PacketIDFilter:第一个参数是注册的特定的ID,第二个参数是包类型的过滤器用来过滤IQ包
        PacketFilter filter = new AndFilter(new PacketIDFilter(registration.getPacketID()), new PacketTypeFilter(Registration.class));
        // 创建一个包收集器，里面的参数是一个你要过滤的数据的过滤器
//        PacketCollector collector = mConnection.createPacketCollector(filter);
//        mConnection.sendPacket(registration);

        Command command = new Command();
        command.listener = new PacketListener() {
            @Override
            public void onSuccess(PacketCollector collector) {
//                Packet packet = null;
//                while ((packet = collector.nextResult()) != null){
//                    L.d(packet.toXML());
//                }

                // 得到IQ包返回的结果
                Registration result = (Registration) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
                L.d(result.getInstructions());
//                // Stop queuing results停止请求results（是否成功的结果）
//                collector.cancel();
//                L.d("result.isRegistered() " + result.isRegistered());
//                if(result == null){
//                    // 失败
//                    L.e("register error: result is null.");
//                    return;
//                }
//                if(result.getType() == IQ.Type.RESULT){
//                    // 注册成功
//                    L.i("register success: " + collector.toString());
//                    return;
//                }
//                XMPPError error = result.getError();
//                if(error == null){
//                    // 失败
//                    L.e("register error: result code error is null.");
//                    return;
//                }
//                int code = error.getCode();
//                L.e("register error: error code " + code);
//                if(code == 409){
//                    // 账号已存在
//                }
            }

            @Override
            public void onFailure(String error) {
                L.d("register error: " + error);
            }
        };
        command.filter = filter;
        command.packet = registration;
        command.des = "注册用户";

        enqueue(command);
    }

    /**
     * 用户登陆
     * @param name 用户名
     * @param password 密码
     * @param resource 登录设备
     * @param listener
     */
    public void login(final String name, final String password, final String resource, final SimpleListener listener) {
        mExecutors.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnection.login(name, password, resource);
                    setOnlineState(Presence.Type.available);
                    publishSimpleSuccess(listener, "logined");
                    L.i("login successed.");
                } catch (Exception e) {
                    publishSimpleFailure(listener, e.toString());
                    L.e("login error: " + e.toString());
                }
            }
        });
    }

    /**
     * 设置用户状态
     * @param type
     */
    public void setOnlineState(Presence.Type type){
        Presence presence = new Presence(type);
        if(type == Presence.Type.available){
            presence.setStatus("在线");
            presence.setPriority(1);
        }else{
            presence.setStatus("其他");
            presence.setPriority(2);
        }
        Command command = new Command();
        command.des = "更改在线状态";
        command.packet = presence;
        PacketFilter filter = new PacketIDFilter(presence.getPacketID());
        command.filter = filter;
        command.listener = null;
        enqueue(command);
    }
}
