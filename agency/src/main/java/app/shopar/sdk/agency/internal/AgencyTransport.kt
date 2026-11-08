package app.shopar.sdk.agency.internal

import app.shopar.sdk.proto.creator.v1.CreatorSDKServiceGrpc
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import java.io.Closeable

internal class AgencyTransport(
    host: String,
    port: Int,
    tls: Boolean,
    agent: ShoparAgent,
    tokenStore: AgencyTokenStore,
) : Closeable {
    private val channel = OkHttpChannelBuilder.forAddress(host, port).apply {
        if (tls) useTransportSecurity() else usePlaintext()
        intercept(ShoparAgentInterceptor(agent), AgencyAuthInterceptor(tokenStore))
    }.build()

    val stub = CreatorSDKServiceGrpc.newBlockingStub(channel)
    fun stubWithAccessToken(token: String) = stub.withOption(ACCESS_TOKEN_OVERRIDE, token)

    override fun close() { channel.shutdownNow() }
}

private class ShoparAgentInterceptor(private val agent: ShoparAgent) : ClientInterceptor {
    override fun <RequestT : Any?, ResponseT : Any?> interceptCall(
        method: MethodDescriptor<RequestT, ResponseT>, callOptions: CallOptions, next: Channel,
    ): ClientCall<RequestT, ResponseT> {
        val call = next.newCall(method, callOptions)
        return object : ForwardingClientCall.SimpleForwardingClientCall<RequestT, ResponseT>(call) {
            override fun start(responseListener: Listener<ResponseT>, headers: Metadata) {
                headers.put(SHOPAR_AGENT, agent.header())
                super.start(responseListener, headers)
            }
        }
    }
}

private class AgencyAuthInterceptor(private val tokenStore: AgencyTokenStore) : ClientInterceptor {
    override fun <RequestT : Any?, ResponseT : Any?> interceptCall(
        method: MethodDescriptor<RequestT, ResponseT>, callOptions: CallOptions, next: Channel,
    ): ClientCall<RequestT, ResponseT> {
        val call = next.newCall(method, callOptions)
        val fullMethod = method.fullMethodName
        val publicAuthMethod = fullMethod.endsWith("/StartAgencySession") ||
            fullMethod.endsWith("/ConfirmAgencySession") || fullMethod.endsWith("/RefreshAgencySession")
        val token = if (publicAuthMethod) null else callOptions.getOption(ACCESS_TOKEN_OVERRIDE) ?: tokenStore.read()?.accessToken
        return object : ForwardingClientCall.SimpleForwardingClientCall<RequestT, ResponseT>(call) {
            override fun start(responseListener: Listener<ResponseT>, headers: Metadata) {
                if (!token.isNullOrBlank()) headers.put(AUTHORIZATION, "Bearer $token")
                super.start(responseListener, headers)
            }
        }
    }
}

private val SHOPAR_AGENT: Metadata.Key<String> = Metadata.Key.of("shopar-agent", Metadata.ASCII_STRING_MARSHALLER)
private val AUTHORIZATION: Metadata.Key<String> = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
private val ACCESS_TOKEN_OVERRIDE: CallOptions.Key<String> = CallOptions.Key.create("shopar-agency-explicit-session-token")
