package com.qcloud.scf.runtime;

/*
 * Vendored from com.tencentcloudapi:scf-java-events:0.0.4 (https://github.com/tencentyun/scf-java-libs),
 * licensed under the Apache License, Version 2.0. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

public interface Context {
    String getRequestId();

    int getTimeLimitInMs();

    int getMemoryLimitInMb();
}
