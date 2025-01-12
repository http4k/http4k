package org.http4k.lens

import org.http4k.lens.ParamMeta.ObjectParam

class JsonRpcMapping<IN : Any, OUT>(convert: (IN) -> OUT) :
    Lens<IN, OUT>(Meta(true, "JSON-RPC", ObjectParam, "mapping", null, emptyMap()), convert)
