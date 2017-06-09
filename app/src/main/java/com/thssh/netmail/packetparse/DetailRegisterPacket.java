package com.thssh.netmail.packetparse;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <iq id="1GRGx-0" from="docmail.cn" type="result">
 * <query xmlns="jabber:iq:register">
 * <name/>
 * <email/>
 * <username/>
 * <password/>
 * <x xmlns="jabber:x:data" type="form">
 * <title>XMPP Client Registration</title>
 * <instructions>Please provide the following information</instructions>
 * <field var="FORM_TYPE" type="hidden">
 * <value>jabber:iq:register</value>
 * </field>
 * <field label="Username" var="username" type="text-single">
 * <required/>
 * </field>
 * <field label="Full name" var="name" type="text-single"/>
 * <field label="Email" var="email" type="text-single"/>
 * <field label="Password" var="password" type="text-private">
 * <required/>
 * </field>
 * </x>
 * </query>
 * </iq>
 */

/**
 * @author zhangyugehu
 * @version V1.0
 * @data 2017/06/09
 */

public class DetailRegisterPacket extends IQ {
    private String instructions = null;
    private Map<String, String> attributes = new HashMap();
    private List<String> requiredFields = new ArrayList();
    private boolean registered = false;
    private boolean remove = false;

    public DetailRegisterPacket() {
    }

    public String getInstructions() {
        return this.instructions;
    }

    public void setInstructions(String var1) {
        this.instructions = var1;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(Map<String, String> var1) {
        this.attributes = var1;
    }

    public List<String> getRequiredFields() {
        return this.requiredFields;
    }

    public void addAttribute(String var1, String var2) {
        this.attributes.put(var1, var2);
    }

    public void setRegistered(boolean var1) {
        this.registered = var1;
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public String getField(String var1) {
        return (String)this.attributes.get(var1);
    }

    public List<String> getFieldNames() {
        return new ArrayList(this.attributes.keySet());
    }

    public void setUsername(String var1) {
        this.attributes.put("username", var1);
    }

    public void setPassword(String var1) {
        this.attributes.put("password", var1);
    }

    public void setRemove(boolean var1) {
        this.remove = var1;
    }

    public String getChildElementXML() {
        StringBuilder var1 = new StringBuilder();
        var1.append("<query xmlns=\"jabber:iq:register\">");
        if(this.instructions != null && !this.remove) {
            var1.append("<instructions>").append(this.instructions).append("</instructions>");
        }

        if(this.attributes != null && this.attributes.size() > 0 && !this.remove) {
            Iterator var2 = this.attributes.keySet().iterator();

            while(var2.hasNext()) {
                String var3 = (String)var2.next();
                String var4 = (String)this.attributes.get(var3);
                var1.append("<").append(var3).append(">");
                var1.append(var4);
                var1.append("</").append(var3).append(">");
            }
        } else if(this.remove) {
            var1.append("</remove>");
        }

        var1.append(this.getExtensionsXML());
        var1.append("</query>");
        return var1.toString();
    }
}
