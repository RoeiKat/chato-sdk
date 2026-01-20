package com.chato.sdk.data

import com.chato.sdk.net.dto.SdkConfigRes
import java.util.concurrent.atomic.AtomicReference

object RemoteConfigStore {
    private val ref = AtomicReference<SdkConfigRes?>(null)

    fun set(cfg: SdkConfigRes) {
        ref.set(cfg)
    }

    fun get(): SdkConfigRes? = ref.get()

    fun clear() {
        ref.set(null)
    }
}
