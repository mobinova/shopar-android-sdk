package app.shopar.sdk.discovery.internal

import ulid.ULID

internal object DiscoveryEventId {
    fun new():String="ipev_${ULID.randomULID()}"
}
