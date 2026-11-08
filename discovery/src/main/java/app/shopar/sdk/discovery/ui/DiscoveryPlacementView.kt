package app.shopar.sdk.discovery.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.shopar.sdk.proto.discovery.v1.DiscoveryPlacementProduct
import app.shopar.sdk.proto.discovery.v1.GetPlacementProductsRequest
import app.shopar.sdk.proto.discovery.v1.OpenProductRequest
import app.shopar.sdk.proto.discovery.v1.PlacementImpression
import app.shopar.sdk.proto.discovery.v1.RecordPlacementImpressionsRequest
import app.shopar.sdk.discovery.internal.DiscoveryEventId
import coil.compose.AsyncImage
import com.google.protobuf.Timestamp
import java.time.Instant
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun DiscoveryPlacementView(
    appId:String,
    placementId:String,
    service:app.shopar.sdk.proto.discovery.v1.DiscoverySDKServiceGrpc.DiscoverySDKServiceBlockingStub,
    modifier:Modifier=Modifier,
    onProductSelected:(String)->Unit,
) {
    val context= LocalContext.current
    val lifecycleOwner= LocalLifecycleOwner.current
    val scope= rememberCoroutineScope()
    var isForeground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var page by remember(placementId){mutableStateOf<PlacementPage?>(null)}
    var loading by remember(placementId){mutableStateOf(true)}
    var openingProduct by remember{mutableStateOf("")}
    var message by remember{mutableStateOf("")}
    var refreshKey by remember(placementId){mutableStateOf(0)}
    val screenViewId= remember(placementId,refreshKey){DiscoveryEventId.new()}

    LaunchedEffect(lifecycleOwner,placementId){
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_RESUME->isForeground=true;Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP->isForeground=false;else->Unit}}
        lifecycleOwner.lifecycle.addObserver(observer)
        try{lifecycleOwner.lifecycle.currentStateFlow.collect{isForeground=it.isAtLeast(Lifecycle.State.RESUMED)}}finally{lifecycleOwner.lifecycle.removeObserver(observer)}
    }
    suspend fun load() {
        loading=true;message=""
        try {
            val request=GetPlacementProductsRequest.newBuilder().setAppId(appId).setPlacementId(placementId).setPageSize(20).setRequestId(DiscoveryEventId.new()).build()
            val result=withContext(Dispatchers.IO){service.getPlacementProducts(request)}
            if(result.outcome=="ok") page=PlacementPage(result.displayFormat,result.configRevision,result.productsList,DiscoveryEventId.new())
            else if(result.outcome=="placement_inactive"){page=PlacementPage(result.displayFormat,result.configRevision,emptyList(),DiscoveryEventId.new());message="Bu ürün alanı şu anda kullanılamıyor."}
            else {page=null;message="Ürün alanı güncellendi. Yenile ve tekrar dene."}
        } catch (error:Throwable){message=error.discoveryMessage()}
        finally{loading=false}
    }
    LaunchedEffect(placementId,refreshKey){load()}

    val current=page
    if(loading&&current==null){Box(modifier.fillMaxWidth().padding(28.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()};return}
    if(current==null){Column(modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(message.ifBlank{"Shopar ürünleri yüklenemedi."},style=MaterialTheme.typography.bodyMedium);OutlinedButton(onClick={refreshKey++}){Text("Tekrar dene")}};return}
    if(current.products.isEmpty()){
        if(current.format!="full_page_feed"){Spacer(modifier.fillMaxWidth().height(0.dp));return}
        Column(modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Şu anda gösterilecek ürün yok.",style=MaterialTheme.typography.titleMedium);Text("Daha sonra tekrar deneyebilirsin.",style=MaterialTheme.typography.bodySmall);OutlinedButton(onClick={refreshKey++}){Text("Yenile")}}
        return
    }
    val viewport= remember(current.screenViewId){mutableStateOf<Rect?>(null)}
    val containerModifier=modifier.onGloballyPositioned{viewport.value=it.boundsInWindow()}
    when(current.format){
        "full_page_feed"->LazyColumn(modifier=containerModifier.fillMaxSize(),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            items(current.products,key={it.productId}){product->DiscoveryProductCard(appId,placementId,current,product,service,viewport.value,isForeground,openingProduct,{openingProduct=it},{message=it;refreshKey++},{productId->onProductSelected(productId)})}
        }
        "horizontal_rail"->Column(modifier=containerModifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)){
            LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                items(current.products,key={it.productId}){product->DiscoveryProductCard(appId,placementId,current,product,service,viewport.value,isForeground,openingProduct,{openingProduct=it},{message=it;refreshKey++},{productId->onProductSelected(productId)},Modifier.width(265.dp))}
            }
            if(message.isNotBlank())Text(message,Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.bodySmall)
        }
        else->DiscoveryProductCard(appId,placementId,current,current.products.first(),service,viewport.value,isForeground,openingProduct,{openingProduct=it},{message=it;refreshKey++},{productId->onProductSelected(productId)},containerModifier.fillMaxWidth().padding(12.dp))
    }
    if(message.isNotBlank()&&current.format!="horizontal_rail")Text(message,Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
}

@Composable
private fun DiscoveryProductCard(
    appId:String,placementId:String,page:PlacementPage,product:DiscoveryPlacementProduct,
    service:app.shopar.sdk.proto.discovery.v1.DiscoverySDKServiceGrpc.DiscoverySDKServiceBlockingStub,viewport:Rect?,isForeground:Boolean,
    openingProduct:String,onOpening:(String)->Unit,onStale:(String)->Unit,onSelected:(String)->Unit,
    modifier:Modifier=Modifier,
) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var cardBounds by remember(product.productId,page.screenViewId){mutableStateOf<Rect?>(null)}
    var impressionSent by remember(product.productId,page.screenViewId){mutableStateOf(false)}
    val seenRequestId=remember(product.productId,page.screenViewId){DiscoveryEventId.new()}
    val clipped=cardBounds?.let{intersect(it,viewport)}
    val visibleRatio=if(clipped==null||cardBounds==null||cardBounds!!.height<=0||cardBounds!!.width<=0)0f else clipped.width*clipped.height/(cardBounds!!.width*cardBounds!!.height)
    val visible=isForeground&&visibleRatio>=0.5f

    LaunchedEffect(visible,isForeground,page.configRevision,product.productId,page.screenViewId){
        if(!visible||impressionSent)return@LaunchedEffect
        delay(1000)
        if(!visible||!isForeground||impressionSent)return@LaunchedEffect
        val event=PlacementImpression.newBuilder().setEventId(seenRequestId).setScreenViewId(page.screenViewId)
            .setProductId(product.productId).setOfferRef(product.offerRef).setVisibleDurationMs(1000)
            .setObservedAt(Timestamp.newBuilder().setSeconds(Instant.now().epochSecond)).build()
        while(!impressionSent&&visible&&isForeground){
            try {
                val request=RecordPlacementImpressionsRequest.newBuilder().setAppId(appId).setPlacementId(placementId).setConfigRevision(page.configRevision).addImpressions(event).build()
                withContext(Dispatchers.IO){service.recordPlacementImpressions(request)}
                impressionSent=true
            } catch (_:Throwable){delay(5000)}
        }
    }

    Card(modifier=modifier.onGloballyPositioned{cardBounds=it.boundsInWindow()},shape=MaterialTheme.shapes.large,
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Column {
            if(product.productImageUrl.isNotBlank())AsyncImage(model=product.productImageUrl,contentDescription=product.productTitle,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(190.dp))
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Text(product.productTitle,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
                Text("${product.merchantName}${product.sellerName.takeIf(String::isNotBlank)?.let{" · $it"}.orEmpty()}",style=MaterialTheme.typography.bodySmall)
                Text(formatMoney(product.displayedPrice.minorUnits,product.displayedPrice.currency),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                Text(product.disclosureText,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Button(enabled=openingProduct!=product.productId,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,colors=discoveryPrimaryButtonColors(),onClick={
                    onOpening(product.productId)
                    scope.launch {
                        val eventId=DiscoveryEventId.new();val requestId=DiscoveryEventId.new()
                        try {
                            val request=OpenProductRequest.newBuilder().setAppId(appId).setPlacementId(placementId).setConfigRevision(page.configRevision)
                                .setProductId(product.productId).setOfferRef(product.offerRef).setPriceObservationId(product.priceObservationId)
                                .setRequestId(requestId).setEventId(eventId).setScreenViewId(page.screenViewId).build()
                            val response=withContext(Dispatchers.IO){service.openProduct(request)}
                            when(response.outcome){
                                "opened"->{onSelected(product.productId);context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(response.redirectUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
                                "offer_changed"->onStale("Fiyat veya teklif güncellendi. Ürün listesini yeniliyoruz.")
                                "offer_unavailable"->onStale("Bu teklif artık kullanılamıyor. Ürün listesini yeniliyoruz.")
                                else->onStale("Ürün alanı güncellendi. Yenile ve tekrar dene.")
                            }
                        } catch (error:Throwable){onStale(error.discoveryMessage())}
                        finally{onOpening("")}
                    }
                }){Text(if(openingProduct==product.productId)"Açılıyor…" else product.ctaText.ifBlank{"Mağazaya git"})}
            }
        }
    }
}

private data class PlacementPage(val format:String,val configRevision:Long,val products:List<DiscoveryPlacementProduct>,val screenViewId:String)
private fun Throwable.discoveryMessage():String{
    val status=when(this){is StatusException->status;is StatusRuntimeException->status;else->null}
    return if(status?.code==Status.Code.RESOURCE_EXHAUSTED)"Shopar'a kısa sürede çok fazla istek gönderildi. Biraz bekleyip tekrar dene." else "Shopar ürünleri şu anda yüklenemiyor. Bağlantını kontrol edip tekrar dene."
}
private fun intersect(rect:Rect,viewport:Rect?):Rect?{if(viewport==null)return null;val left=maxOf(rect.left,viewport.left);val top=maxOf(rect.top,viewport.top);val right=minOf(rect.right,viewport.right);val bottom=minOf(rect.bottom,viewport.bottom);return if(right>left&&bottom>top)Rect(left,top,right,bottom)else null}
private fun formatMoney(minorUnits:Long,currency:String):String=java.text.NumberFormat.getCurrencyInstance().format(minorUnits/100.0).let{formatted->if(currency.isBlank())formatted else "$formatted $currency"}
