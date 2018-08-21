package org.http4k.jsonrpc

import org.http4k.lens.Lens
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta

class Params<NODE, IN>(convert: (NODE) -> IN) : Lens<NODE, IN>(
        Meta(true, "request", ParamMeta.ObjectParam, "params"), convert)

class Result<OUT, NODE>(convert: (OUT) -> NODE) : Lens<OUT, NODE>(
        Meta(true, "response", ParamMeta.ObjectParam, "result"), convert)