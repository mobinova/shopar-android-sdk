package app.shopar.sdk.agency.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> grpcCall(block: () -> T): T = withContext(Dispatchers.IO) { block() }
