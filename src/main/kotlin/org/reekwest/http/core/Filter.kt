package org.reekwest.http.core

interface Filter<in ReqIn, out RespOut, out ReqOut, in RespIn> :
    Function2<ReqIn, Service<ReqOut, RespIn>, RespOut> {

    fun andThen(service: Service<ReqOut, RespIn>): Service<ReqIn, RespOut> = { invoke(it, service) }

    fun <Req2Out, Resp2In> andThen(next: Filter<ReqOut, RespIn, Req2Out, Resp2In>):
        Filter<ReqIn, RespOut, Req2Out, Resp2In>
        = mk { req, svc -> invoke(req, { next(it, svc) }) }

    companion object {

        fun <Req, Resp> noOp() = object : Filter<Req, Resp, Req, Resp> {
            override fun invoke(req: Req, next: Service<Req, Resp>): Resp = next(req)
        }

        fun <ReqIn, RespOut, ReqOut, RespIn> mk(fn: (ReqIn, Service<ReqOut, RespIn>) -> RespOut)
            = object : Filter<ReqIn, RespOut, ReqOut, RespIn> {
            override fun invoke(req: ReqIn, next: Service<ReqOut, RespIn>): RespOut = fn(req, next)
        }
    }
}
