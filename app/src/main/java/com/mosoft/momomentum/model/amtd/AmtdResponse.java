package com.mosoft.momomentum.model.amtd;


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
}
