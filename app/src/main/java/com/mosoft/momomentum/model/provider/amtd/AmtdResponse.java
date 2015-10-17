package com.mosoft.momomentum.model.provider.amtd;


import com.mosoft.momomentum.model.provider.Interfaces;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/*

<amtd>
    <result></result>
    <.../>
</amtd>
 */

@Root(name = "amtd", strict = false)
public class AmtdResponse {

    @Element
    private String result;

    @Element(required = false)
    private String error;

    public boolean succeeded() {
        return "OK".equalsIgnoreCase(result);
    }

    public String getError() {
        return error;
    }

    public Interfaces.Provider getProvider() {
        return Interfaces.Provider.AMERITRADE;
    }
}
