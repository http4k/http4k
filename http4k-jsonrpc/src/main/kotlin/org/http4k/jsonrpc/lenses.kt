package org.http4k.jsonrpc

import org.http4k.lens.Lens
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam

class Mapping<IN : Any, OUT>(convert: (IN) -> OUT) : Lens<IN, OUT>(Meta(true, "JSON-RPC", ObjectParam, "mapping"), convert)
