package app.shopar.sdk.discovery.internal

import app.shopar.sdk.proto.discovery.v1.DiscoverySDKServiceGrpc
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import java.io.Closeable

/** Anonymous Discovery channel. This class has no credential store or auth interceptor. */
internal class DiscoveryTransport(host:String,port:Int,tls:Boolean,private val agent:ShoparAgent):Closeable{
    private val channel=OkHttpChannelBuilder.forAddress(host,port).apply{
        if(tls)useTransportSecurity() else usePlaintext()
        intercept(DiscoveryAgentInterceptor(agent))
    }.build()
    val stub=DiscoverySDKServiceGrpc.newBlockingStub(channel)
    fun setOptionalDeviceMetadataCollectionEnabled(enabled:Boolean)=agent.setOptionalDeviceMetadataCollectionEnabled(enabled)
    fun resetInstallationIdentity()=agent.resetInstallationIdentity()
    override fun close(){channel.shutdownNow()}
}

private class DiscoveryAgentInterceptor(private val agent:ShoparAgent):ClientInterceptor{
    override fun <RequestT:Any?,ResponseT:Any?> interceptCall(method:MethodDescriptor<RequestT,ResponseT>,callOptions:CallOptions,next:Channel):ClientCall<RequestT,ResponseT>{
        val call=next.newCall(method,callOptions)
        return object:ForwardingClientCall.SimpleForwardingClientCall<RequestT,ResponseT>(call){
            override fun start(responseListener:Listener<ResponseT>,headers:Metadata){headers.put(SHOPAR_AGENT,agent.header());super.start(responseListener,headers)}
        }
    }
}
private val SHOPAR_AGENT:Metadata.Key<String> = Metadata.Key.of("shopar-agent",Metadata.ASCII_STRING_MARSHALLER)
