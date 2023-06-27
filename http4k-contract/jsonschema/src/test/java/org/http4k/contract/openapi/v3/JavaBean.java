package org.http4k.contract.openapi.v3;

import java.io.Serializable;

public class JavaBean implements Serializable {
    private static final long serialVersionUID = 1554583580;
    private final String name;

    public JavaBean(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
