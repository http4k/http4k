package org.http4k.connect.ollama.action

import se.ansman.kotshi.JsonSerializable


@JsonSerializable
data class ModelOptions(
    val num_keep: Long? = null,
    val seed: Long? = null,
    val num_predict: Long? = null,
    val top_k: Long? = null,
    val top_p: Double? = null,
    val tfs_z: Double? = null,
    val typical_p: Double? = null,
    val repeat_last_n: Long? = null,
    val temperature: Double? = null,
    val repeat_penalty: Double? = null,
    val presence_penalty: Double? = null,
    val frequency_penalty: Double? = null,
    val mirostat: Long? = null,
    val mirostat_tau: Double? = null,
    val mirostat_eta: Double? = null,
    val penalize_newline: Boolean? = null,
    val stop: List<String>? = null,
    val numa: Boolean? = null,
    val num_ctx: Long? = null,
    val num_batch: Long? = null,
    val num_gpu: Long? = null,
    val main_gpu: Long? = null,
    val low_vram: Boolean? = null,
    val f16_kv: Boolean? = null,
    val vocab_only: Boolean? = null,
    val use_mmap: Boolean? = null,
    val use_mlock: Boolean? = null,
    val num_thread: Long? = null
)
