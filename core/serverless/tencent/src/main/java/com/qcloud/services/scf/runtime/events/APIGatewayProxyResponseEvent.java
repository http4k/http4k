package com.qcloud.services.scf.runtime.events;

/*
 * Vendored from com.tencentcloudapi:scf-java-events:0.0.4 (https://github.com/tencentyun/scf-java-libs),
 * licensed under the Apache License, Version 2.0. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Class that represents an APIGatewayProxyResponseEvent object
 */
public class APIGatewayProxyResponseEvent implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 4290727616926418509L;

    private Integer statusCode;

    private Map<String, String> headers;

    private String body;

    private Boolean isBase64Encoded;

    /**
     * default constructor
     */
    public APIGatewayProxyResponseEvent() {
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }

    public void setIsBase64Encoded(Boolean isBase64Encoded) {
        this.isBase64Encoded = isBase64Encoded;
    }

    /**
     * Returns a string representation of this object; useful for testing and debugging.
     *
     * @return A string representation of this object.
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("{");
        if (isBase64Encoded != null) {
            out.append("\"isBase64Encoded\":").append(isBase64Encoded.booleanValue());
        }
        if (headers != null) {
            separate(out);
            out.append("\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> header : headers.entrySet()) {
                // http4k header values are nullable; fastjson dropped null-valued entries, so we do too
                if (header.getValue() == null) {
                    continue;
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(out, header.getKey());
                out.append(':');
                writeString(out, header.getValue());
            }
            out.append('}');
        }
        if (body != null) {
            separate(out);
            out.append("\"body\":");
            writeString(out, body);
        }
        if (statusCode != null) {
            separate(out);
            out.append("\"statusCode\":").append(statusCode.intValue());
        }
        return out.append('}').toString();
    }

    private static void separate(StringBuilder out) {
        if (out.length() > 1) {
            out.append(',');
        }
    }

    private static void writeString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    if (c < 0x20 || (c >= 0x7F && c <= 0x9F) || c == 0x2028 || c == 0x2029) {
                        out.append(String.format("\\u%04X", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof APIGatewayProxyResponseEvent == false)
            return false;
        APIGatewayProxyResponseEvent other = (APIGatewayProxyResponseEvent) obj;
        if (other.getStatusCode() == null ^ this.getStatusCode() == null)
            return false;
        if (other.getStatusCode() != null && other.getStatusCode().equals(this.getStatusCode()) == false)
            return false;
        if (other.getHeaders() == null ^ this.getHeaders() == null)
            return false;
        if (other.getHeaders() != null && other.getHeaders().equals(this.getHeaders()) == false)
            return false;
        if (other.getBody() == null ^ this.getBody() == null)
            return false;
        if (other.getBody() != null && other.getBody().equals(this.getBody()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getStatusCode() == null) ? 0 : getStatusCode().hashCode());
        hashCode = prime * hashCode + ((getHeaders() == null) ? 0 : getHeaders().hashCode());
        hashCode = prime * hashCode + ((getIsBase64Encoded() == null) ? 0 : getIsBase64Encoded().hashCode());
        hashCode = prime * hashCode + ((getBody() == null) ? 0 : getBody().hashCode());
        return hashCode;
    }

    @Override
    public APIGatewayProxyResponseEvent clone() {
        try {
            return (APIGatewayProxyResponseEvent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone()", e);
        }
    }

}
