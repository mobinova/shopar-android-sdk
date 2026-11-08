package app.shopar.sdk.agency.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.shopar.sdk.agency.AgencyCreatorSession
import app.shopar.sdk.agency.ShoparAgency
import app.shopar.sdk.proto.creator.v1.AgencyProductOrDeal
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun AgencySignInScreen(onSignedIn: (AgencyCreatorSession) -> Unit, modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var requestId by remember { mutableStateOf("") }
    var expiresIn by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<AgencyCreatorSession?>(null) }
    var explanationVisible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val disclosure = result?.referralDisclosure

    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Shopar hesabına giriş yap", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Link oluşturup performansını bu uygulamadan yönetebilirsin.", style = MaterialTheme.typography.bodyMedium)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        if (result == null) {
            OutlinedTextField(email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("E-posta") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            Button(enabled = !busy && email.contains('@'), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = agencyPrimaryButtonColors(), onClick = {
                busy = true; error = ""
                scope.launch { runCatching { ShoparAgency.startSignIn(email) }.onSuccess { challenge -> requestId = challenge.requestId; expiresIn = challenge.expiresInSeconds }.onFailure { error = "Giriş kodu gönderilemedi. Biraz sonra tekrar dene." }; busy = false }
            }) { if (busy) CircularProgressIndicator() else Text("Doğrulama kodu gönder") }
            if (requestId.isNotEmpty()) {
                Text("E-postana gönderilen 6 haneli kodu gir. Kod ${expiresIn / 60} dakika geçerli.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, modifier = Modifier.fillMaxWidth(), label = { Text("Doğrulama kodu") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                Button(enabled = !busy && code.length == 6, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = agencyPrimaryButtonColors(), onClick = {
                    busy = true; error = ""
                    scope.launch { runCatching { ShoparAgency.confirmSignIn(requestId, code) }.onSuccess { result = it }.onFailure { error = "Kod doğrulanamadı. Kontrol edip tekrar dene." }; busy = false }
                }) { if (busy) CircularProgressIndicator() else Text("Giriş yap") }
            }
        } else {
            Text("Merhaba, ${result?.fullName.orEmpty().ifBlank { "Shopar kullanıcısı" }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (disclosure?.relationship == "referred_to_another_agency") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Shopar hesabın başka bir ajansla ilişkilendirilmiş.", fontWeight = FontWeight.SemiBold)
                        Text("Bu uygulamadaki Shopar oturumunla hesabına giriş yapabilirsin. Bu, ajansın panel erişimini değiştirmez.", style = MaterialTheme.typography.bodySmall)
                        if (disclosure.referralAgencyLabel.isNotBlank()) Text("Yönlendiren ajans: ${disclosure.referralAgencyLabel}", style = MaterialTheme.typography.bodySmall)
                        if (explanationVisible) Text("Yönlendiren ajans, mevcut Shopar referral kuralları kapsamında ilişkilendirilir. Bu uygulamanın oturumu ve uygulama paneli erişimi ayrı yetkilendirilir.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { explanationVisible = !explanationVisible }) { Text(if (explanationVisible) "Daha az bilgi" else "Bu ne demek?") }
                    }
                }
            }
            Text("Devam ederek Shopar hesabını bu uygulamada kullanırsın.", style = MaterialTheme.typography.bodySmall)
            Button(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = agencyPrimaryButtonColors(), onClick = { result?.let(onSignedIn) }) { Text("Devam et") }
        }
    }
}

@Composable
fun AgencyLinksScreen(
    onShare: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenAccountSecurity: (() -> Unit)? = null,
) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val creator by ShoparAgency.creatorSession.collectAsState()
    var showCreatorHome by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var catalog by remember { mutableStateOf(emptyList<AgencyProductOrDeal>()) }
    var links by remember { mutableStateOf(emptyList<app.shopar.sdk.proto.creator.v1.AgencyCreatorLink>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showSessions by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf(emptyList<app.shopar.sdk.proto.creator.v1.AgencySDKSession>()) }
    var sessionsLoading by remember { mutableStateOf(false) }
    var sessionsError by remember { mutableStateOf("") }

    suspend fun reload() {
        busy = true
        runCatching { ShoparAgency.myLinks() }.onSuccess { links = it }.onFailure { error = "Paylaşımlar yüklenemedi. Bağlantını kontrol edip tekrar dene." }
        busy = false
    }

    LaunchedEffect(Unit) { runCatching { ShoparAgency.restoreSession() }.getOrNull()?.let { showCreatorHome=true;reload() } }
    LaunchedEffect(creator) {
        links= emptyList();catalog= emptyList();query="";error="";busy=false
        sessions = emptyList(); sessionsError = ""; sessionsLoading = false; showSessions = false
        if (creator == null) showCreatorHome=false
        else if (showCreatorHome) reload()
    }
    // Keep the SDK-owned post-OTP relationship notice visible until the user taps Continue.
    if (creator == null || !showCreatorHome) {
        AgencySignInScreen(onSignedIn = { showCreatorHome=true;scope.launch { reload() } }, modifier = modifier)
        return
    }
    suspend fun reloadSessions() {
        sessionsLoading = true
        sessionsError = ""
        runCatching { ShoparAgency.agencySdkSessions().sessionsList }
            .onSuccess { sessions = it }
            .onFailure { sessionsError = "Bu uygulamadaki cihazlar yüklenemedi. Tekrar dene." }
        sessionsLoading = false
    }
    if (showSessions) {
        AlertDialog(
            onDismissRequest = { showSessions = false },
            title = { Text("Bu uygulamadaki cihazlar") },
            text = {
                Column(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onOpenAccountSecurity != null) {
                        TextButton(onClick = onOpenAccountSecurity) { Text("Shopar hesabındaki tüm oturumlar") }
                    }
                    if (sessionsLoading && sessions.isEmpty()) CircularProgressIndicator()
                    if (sessionsError.isNotEmpty()) Text(sessionsError, color = MaterialTheme.colorScheme.error)
                    if (!sessionsLoading && sessionsError.isEmpty() && sessions.isEmpty()) Text("Bu uygulamada açık oturum bulunamadı.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sessions, key = { it.sessionId }) { session ->
                            Card {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(session.appName.ifBlank { "Shopar" }, fontWeight = FontWeight.SemiBold)
                                    Text(listOf(session.agencyLabel, session.platform, session.deviceModel).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                    Text(listOf(session.operatingSystem, session.appVersion).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                    if (session.hasLastActiveAt()) Text("Son etkinlik: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(session.lastActiveAt.seconds * 1000))}", style = MaterialTheme.typography.labelSmall)
                                    if (session.isCurrentSession) Text("Bu cihaz · Şu anki oturum", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    else TextButton(enabled = !sessionsLoading, onClick = {
                                        sessionsLoading = true
                                        scope.launch {
                                            runCatching { ShoparAgency.revokeAgencySdkSession(session.sessionId); ShoparAgency.agencySdkSessions().sessionsList }
                                                .onSuccess { sessions = it }
                                                .onFailure { sessionsError = "Oturum kapatılamadı. Tekrar dene." }
                                            sessionsLoading = false
                                        }
                                    }) { Text("Oturumu kapat", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !sessionsLoading, onClick = { scope.launch { reloadSessions() } }) { Text("Yenile") }
            },
            dismissButton = { TextButton(onClick = { showSessions = false }) { Text("Kapat") } },
        )
    }
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Paylaşımlarım", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${creator?.fullName.orEmpty()}${creator?.publicUsername?.takeIf(String::isNotBlank)?.let { " · @$it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
        TextButton(enabled = !sessionsLoading, onClick = { showSessions = true; scope.launch { reloadSessions() } }) { Text("Bu uygulamadaki cihazlar ve oturumlar") }
        if (onOpenAccountSecurity != null) {
            TextButton(onClick = onOpenAccountSecurity) { Text("Shopar hesap güvenliği ve tüm oturumlar") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.weight(1f), label = { Text("Shopar kataloğunda ara") }, singleLine = true)
            Button(enabled = !busy && query.isNotBlank(), shape = MaterialTheme.shapes.medium, colors = agencyPrimaryButtonColors(), onClick = { busy=true;error="";scope.launch { runCatching { ShoparAgency.searchCatalog(query) }.onSuccess { catalog=it }.onFailure { error="Katalog şu anda yüklenemiyor. Tekrar dene." };busy=false } }) { Text("Ara") }
        }
        catalog.forEach { item ->
            Card(modifier=Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                    if(item.imageUrl.isNotBlank())AsyncImage(model=item.imageUrl,contentDescription=null,modifier=Modifier.width(64.dp).height(64.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(item.title,fontWeight=FontWeight.SemiBold)
                        if(item.subtitle.orEmpty().isNotBlank())Text(item.subtitle.orEmpty(),style=MaterialTheme.typography.bodySmall)
                        Text("${item.price.minorUnits / 100.0} ${item.price.currency}",style=MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(enabled=!busy,onClick={busy=true;error="";scope.launch { runCatching { if(item.itemKind=="deal")ShoparAgency.createDealLink(item.itemId,UUID.randomUUID().toString()) else ShoparAgency.createProductLink(item.itemId,UUID.randomUUID().toString()) }.onSuccess { links=listOf(it)+links }.onFailure { error="Shopar linki oluşturulamadı. Tekrar dene." };busy=false } }){Text("Link oluştur")}
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        HorizontalDivider()
        if (links.isEmpty()) Text("Henüz paylaşımın yok. Shopar kataloğundan bir ürün seçip link oluştur.", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(links, key = { it.id }) { link ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (link.productId.isNotBlank()) "Ürün paylaşımı" else "Fırsat paylaşımı", fontWeight = FontWeight.SemiBold)
                            Text(link.shareUrl, style = MaterialTheme.typography.bodySmall)
                            Text("Ziyaretçi ${link.uniqueVisitors} · Mağaza yönlendirmesi ${link.successfulRedirects}", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            if(onShare!=null)onShare(link.shareUrl) else {
                                val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,link.shareUrl)}
                                val chooser=Intent.createChooser(send,"Shopar linkini paylaş")
                                if(context !is Activity)chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            }
                        }) { Text("Paylaş") }
                    }
                }
            }
        }
    }
}
