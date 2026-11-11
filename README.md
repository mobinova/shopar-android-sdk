# Shopar Android SDK

Two Android modules live in this public repository:

- **Agency SDK**: Shopar OTP sign-in, creator profile/social account views, app-scoped links, performance, session management, and native share-sheet handoff.
- **Discovery SDK**: anonymous Product and Deal placements in full-page feed, horizontal rail, or single-card format.

Both modules share one allowlisted, public protobuf/gRPC contract library (`:contracts`). They do not depend on the private `shopar-proto` checkout. The `:sample` host app installs both modules together.

The Android public proto snapshot uses the JVM namespace `app.shopar.sdk.proto.*`, separate from Shopar's first-party `app.shopar.proto.*` bindings. This keeps generated Java/Kotlin classes from colliding when a host already generates first-party contracts. The protobuf wire package and field numbers remain unchanged.

## App registration

Create a separate app property in Agency Dashboard or App Studio for each platform app. Keep the returned `app_id` with that integration. Discovery also needs at least one placement created in App Studio; its `placement_id` is part of the initialization/rendering call for Product or Deal cards. Web properties are not supported yet.

## Installation

The first Android release tag has not been cut yet. Releases are built on JitPack as separate Agency/Discovery AARs, with the shared `contracts` AAR as a transitive dependency. Until the first tag exists, use this repository as an included Gradle build or run the `:sample` application. Do not pin a made-up version in a host app.

Add `maven { url = uri("https://jitpack.io") }` to dependency repositories. The release pipeline uses these multi-module coordinates (with the exact tag created by the release workflow):

```kotlin
implementation("com.github.mobinova.shopar-android-sdk:agency-android:<exact-tag>")
implementation("com.github.mobinova.shopar-android-sdk:discovery-android:<exact-tag>")
```

The two lines are optional independently. The shared protocol contracts library is a transitive dependency. The App Studio Setup / Docs page will show only a release tag that has completed publishing.

## Agency setup

```kotlin
ShoparAgency.initialize(
    context = applicationContext,
    config = AgencyConfiguration(appId = "app_<dashboard-id>"),
)
```

Add `ShoparAgency.SignInScreen` and `ShoparAgency.LinksScreen` where they fit your app. The screen performs email OTP with Shopar; callers receive only a safe profile projection, not access or refresh PWTs. If your host user signs out or changes accounts, call `ShoparAgency.onHostAccountChanged()` to end the local Shopar session and clear account-scoped views. The default link action opens Android's system share sheet; `LinksScreen` also accepts an app-specific share callback. Its **Bu uygulamadaki cihazlar ve oturumlar** screen lists/revokes only sessions from the current Agency app. Pass `onOpenAccountSecurity` to show a route into your Shopar account-security screen, where users can review and revoke sessions across Shopar and other apps; host-app navigation is intentionally caller-owned.

Pass `style = AgencyStyle(...)` to `AgencyConfiguration` to customize the bundled sign-in and links screens. Color values are ARGB `Long`s and radii are `Float` dp values. `AgencyStyle.Midnight` is also available. The style is fixed when the Agency module initializes.

```kotlin
ShoparAgency.initialize(context, AgencyConfiguration(
    appId = "app_<dashboard-id>",
    style = AgencyStyle(accentColor = 0xFFD8F36AL, cardCornerRadiusDp = 18f),
))
```

## Discovery setup

Create the first placement before generating the full render snippet:

```kotlin
ShoparDiscovery.initialize(
    context = applicationContext,
    config = DiscoveryConfiguration(appId = "app_<dashboard-id>"),
)

ShoparDiscovery.Placement(placementId = "plc_<app-studio-id>")
```

Discovery does not sign consumers into Shopar or attach Agency tokens. The backend response selects Product and Deal cards, the exact merchant destination, and placement format. Within the manual catalog rule, selected Products retain their order and precede selected Deals; the owner may place this rule before or after Interest rules. Shopar's Product recommendation fallback always runs last. Product cards open the exact ProductListing destination; Deal cards open the exact Deal destination. The SDK counts an impression only after a card is at least 50% visible for one continuous foreground second. It records the item-specific destination reference and opens Shopar's redirect URL; raw affiliate targets are not returned to the host. Hosts may optionally observe selected Deal IDs with `onDealSelected`; navigation remains SDK-owned.

`collectDeviceMetadata` sets the initial preference. Call `ShoparDiscovery.setOptionalDeviceMetadataCollectionEnabled(enabled)` whenever the host privacy preference changes; `false` removes optional model/OS context from future requests without rotating the `dev_` installation ID. There is no queued optional context to drain. Required app/placement/event attribution continues. The random installation ID is not a person identifier.

Call `ShoparDiscovery.resetInstallationIdentity()` only after a user explicitly requests an identity reset. This replaces the app-private, no-backup `dev_` ID; prior server history remains separate. Uninstall/data clearing also removes the local ID and results in a new one.

Pass `style = DiscoveryStyle(...)` to `DiscoveryConfiguration` to customize Product/Deal cards and controls. The style exposes ARGB colors, card/control corner radii, and a `DiscoveryStyle.Midnight` palette. It applies to the SDK's bundled placement UI; the server still controls placement format, selected Product/Deal/offer, and order. Configure the style before the first `ShoparDiscovery.initialize` call.

```kotlin
ShoparDiscovery.initialize(context, DiscoveryConfiguration(
    appId = "app_<dashboard-id>",
    style = DiscoveryStyle(accentColor = 0xFFD8F36AL, cardCornerRadiusDp = 18f),
))
```

## Examples and privacy

See [privacy and Data safety guidance](docs/privacy-and-data-safety.md). Build the sample with:

```sh
./gradlew :sample:assembleDebug
```

Set `agencyAppId`, `discoveryAppId`, and `discoveryPlacementId` Gradle properties to real dashboard IDs when running the sample. The fallback IDs only let the sample compile and cannot serve live data.
