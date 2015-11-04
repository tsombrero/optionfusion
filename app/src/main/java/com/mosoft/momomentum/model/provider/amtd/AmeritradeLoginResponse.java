package com.mosoft.momomentum.model.provider.amtd;

import com.mosoft.momomentum.client.ClientInterfaces;
import com.mosoft.momomentum.model.provider.Interfaces;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

/*

General Structure of XML Response
<?xml version="1.0" ?>
<amtd>
    <result></result>
    <xml-log-in>
        <session-id></session-id>
        <user-id></user-id>
        <cdi></cdi>
        <timeout></timeout>
        <login-time></login-time>
        <associated-account-id>
        </associated-account-id>
        <nyse-quotes></nyse-quotes>
        <nasdaq-quotes></nasdaq-quotes>
        <opra-quotes></opra-quotes>
        <amex-quotes></amex-quotes>
        <exchange-status></exchange-status>
        <accounts>
            <account>
                <account-id></account-id>
                <display-name></display-name>
                <cdi></cdi>
                <description></description>
                <associated-account></associated-account>
                <company></company>
                <segment></segment>
                <unified></unified>
                <preferences>
                    <express-trading></express-trading>
                    <option-direct-routing></option-direct-routing>
                    <stock-direct-routing></stock-direct-routing>
                </preferences>
                <authorizations>
                    <apex></apex>
                    <level2></level2>
                    <stock-trading></stock-trading>
                    <margin-trading></margin-trading>
                    <streaming-news></streaming-news>
                    <option-trading></option-trading>
                    <streamer></streamer>
                    <advanced-margin></advanced-margin>
                </authorizations>
            </account>
            <account> ......
            </account>
        </accounts>
    </xml-log-in>
</amtd>

 */

public class AmeritradeLoginResponse extends AmtdResponseBase implements ClientInterfaces.LoginResponse {

    @Element(name = "xml-log-in", required = false)
    private Data data;

    public static class Data {
        @Element(name = "session-id")
        private String sessionId;

        @Element(name = "user-id")
        private String userId;

        @Element(name = "login-time")
        private String loginTime;

        @ElementList
        private List<AmeritradeAccount> accounts;
    }

    @Override
    public String getSessionId() {
        return data.sessionId;
    }

    @Override
    public String getUserId() {
        return data.userId;
    }

    @Override
    public String getLoginTime() {
        return data.loginTime;
    }

    public List<AmeritradeAccount> getAccounts() {
        return data.accounts;
    }
}
