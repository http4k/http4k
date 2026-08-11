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
import java.util.Objects;

/**
 * Class that represents an APIGateway Proxy Request Event
 */
public class APIGatewayProxyRequestEvent implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = -1030954810507877056L;

    private ProxyRequestContext requestContext;

    private Map<String, String> headers;

    private String body;

    private Map<String, String> pathParameters;

    private Map<String, String> queryStringParameters;

    private Map<String, String> headerParameters;

    private Map<String, String> stageVariables;

    private String path;

    private Map<String, String> queryString;

    private String httpMethod;

    private Boolean isBase64Encoded;

    /**
     * class that represents proxy request context
     */
    public static class ProxyRequestContext implements Serializable, Cloneable {

        private static final long serialVersionUID = -8498640336401463322L;

        private String serviceId;

        private String path;

        private String httpMethod;

        private String requestId;

        private RequestIdentity identity;

        private String sourceIp;

        private String stage;

        private Boolean isBase64Encoded;

        /**
         * default constructor
         */
        public ProxyRequestContext() {
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public RequestIdentity getIdentity() {
            return identity;
        }

        public void setIdentity(RequestIdentity identity) {
            this.identity = identity;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public void setSourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
        }

        public String getStage() {
            return stage;
        }

        public void setStage(String stage) {
            this.stage = stage;
        }

        public Boolean getIsBase64Encoded() {
            return isBase64Encoded;
        }

        public void setIsBase64Encoded(Boolean isBase64Encoded) {
            this.isBase64Encoded = isBase64Encoded;
        }

        @Override
        public String toString() {
            return "ProxyRequestContext{" +
                "serviceId='" + serviceId + '\'' +
                ", path='" + path + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", requestId='" + requestId + '\'' +
                ", identity=" + identity +
                ", sourceIp='" + sourceIp + '\'' +
                ", stage='" + stage + '\'' +
                ", isBase64Encoded='" + isBase64Encoded + '\'' +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProxyRequestContext that = (ProxyRequestContext) o;
            return Objects.equals(getServiceId(), that.getServiceId()) &&
                Objects.equals(getPath(), that.getPath()) &&
                Objects.equals(getHttpMethod(), that.getHttpMethod()) &&
                Objects.equals(getRequestId(), that.getRequestId()) &&
                Objects.equals(getIdentity(), that.getIdentity()) &&
                Objects.equals(getSourceIp(), that.getSourceIp()) &&
                Objects.equals(getStage(), that.getStage()) &&
                Objects.equals(getIsBase64Encoded(), that.getIsBase64Encoded());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getServiceId(), getPath(), getHttpMethod(), getRequestId(), getIdentity(), getSourceIp(), getStage(), getIsBase64Encoded());
        }

        @Override
        public ProxyRequestContext clone() {
            try {
                return (ProxyRequestContext) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone()", e);
            }
        }

    }

    public static class RequestIdentity implements Serializable, Cloneable {

        private static final long serialVersionUID = 5055827626429971507L;

        private String secretId;

        public RequestIdentity() {
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        @Override
        public String toString() {
            return "RequestIdentity{" +
                "secretId='" + secretId + '\'' +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestIdentity that = (RequestIdentity) o;
            return Objects.equals(getSecretId(), that.getSecretId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getSecretId());
        }

        @Override
        public RequestIdentity clone() {
            try {
                return (RequestIdentity) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone()", e);
            }
        }
    }

    /**
     * default constructor
     */
    public APIGatewayProxyRequestEvent() {
    }

    public ProxyRequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(ProxyRequestContext requestContext) {
        this.requestContext = requestContext;
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

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }

    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }

    public Map<String, String> getHeaderParameters() {
        return headerParameters;
    }

    public void setHeaderParameters(Map<String, String> headerParameters) {
        this.headerParameters = headerParameters;
    }

    public Map<String, String> getStageVariables() {
        return stageVariables;
    }

    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQueryString() {
        return queryString;
    }

    public void setQueryString(Map<String, String> queryString) {
        this.queryString = queryString;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }

    public void setisBase64Encoded(Boolean isBase64Encoded) {
        this.isBase64Encoded = isBase64Encoded;
    }

    @Override
    public String toString() {
        return "APIGatewayProxyRequestEvent{" +
            "requestContext=" + requestContext +
            ", headers=" + headers +
            ", body='" + body + '\'' +
            ", pathParameters=" + pathParameters +
            ", queryStringParameters=" + queryStringParameters +
            ", headerParameters=" + headerParameters +
            ", stageVariables=" + stageVariables +
            ", path='" + path + '\'' +
            ", queryString=" + queryString +
            ", httpMethod='" + httpMethod + '\'' +
            ", isBase64Encoded='" + isBase64Encoded + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        APIGatewayProxyRequestEvent that = (APIGatewayProxyRequestEvent) o;
        return Objects.equals(getRequestContext(), that.getRequestContext()) &&
            Objects.equals(getHeaders(), that.getHeaders()) &&
            Objects.equals(getBody(), that.getBody()) &&
            Objects.equals(getPathParameters(), that.getPathParameters()) &&
            Objects.equals(getQueryStringParameters(), that.getQueryStringParameters()) &&
            Objects.equals(getHeaderParameters(), that.getHeaderParameters()) &&
            Objects.equals(getStageVariables(), that.getStageVariables()) &&
            Objects.equals(getPath(), that.getPath()) &&
            Objects.equals(getQueryString(), that.getQueryString()) &&
            Objects.equals(getHttpMethod(), that.getHttpMethod()) &&
            Objects.equals(getIsBase64Encoded(), that.getIsBase64Encoded());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRequestContext(), getHeaders(), getBody(), getPathParameters(), getQueryStringParameters(), getHeaderParameters(), getStageVariables(), getPath(), getQueryString(), getHttpMethod(), getIsBase64Encoded());
    }

    @Override
    public APIGatewayProxyRequestEvent clone() {
        try {
            return (APIGatewayProxyRequestEvent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone()", e);
        }
    }
}
