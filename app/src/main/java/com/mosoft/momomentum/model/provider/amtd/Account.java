package com.mosoft.momomentum.model.provider.amtd;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/*

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

 */

@Root(name = "account")
public class Account {

    @Element(name = "account-id")
    private String accountId;

    @Element(name = "display-name")
    private String displayName;

    @Element
    private String description;

    @Element(name = "associated-account")
    private String associatedAccount;


}