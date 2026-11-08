# Privacy and data safety

The host app remains the Android security boundary. SDK APIs and callbacks never expose Agency access or refresh PWTs. Agency PWTs are stored in SDK-private, no-backup app storage, protected by Android's normal application sandbox. The SDK does not wrap tokens in custom encryption. Modified host-app code running in the same process is outside the SDK's isolation guarantees.

## Agency module

| Data | Required | Purpose | Sent when |
|---|---:|---|---|
| Random `dev_` installation ID | Yes | Distinguish this SDK installation and bind the Agency SDK session to its device record | SDK RPCs and login |
| Platform, SDK version, package/bundle ID | Yes | App registration and compatibility/support | SDK RPCs |
| Device model, OS version, locale, timezone | Yes for Agency sessions | Show safe device/session metadata and troubleshoot a creator's own sessions | Login and SDK RPCs |
| Email address | Yes | Shopar email OTP account authentication | Login only |
| Creator name, public username, profile image, social handles | Optional user profile fields | Render the creator's account and existing public social profile in the Agency app | When displayed or changed |
| Link, performance, and referral activity | Yes for those features | Existing Shopar link attribution, creator reporting, and Agency referral reporting | When the creator uses these features |

The Agency session grants only the server-enforced Creator SDK RPC surface. Referral ownership and Agency Dashboard permissions are separate. Signing out or switching the host app's own account must call `ShoparAgency.onHostAccountChanged()`; this revokes the current Agency session and clears local account state. The first-party Shopar account-security screen manages sessions for other apps.

## Discovery module

| Data | Required | Purpose | Sent when |
|---|---:|---|---|
| Random `dev_` installation ID | Yes | Deduplicate retries and report installation traffic without creating a Shopar account session | Placement request, impression, and product open |
| `app_id`, `placement_id`, config revision | Yes | Select the registered placement and report its performance | Every placement operation |
| Product/offer references, screen-view and event IDs | Yes for the corresponding event | Render the exact offer, deduplicate impressions, and create a safe attributed redirect | Placement response, visible impression, product tap |
| Platform and SDK version | Yes | App compatibility and reporting | Placement requests |
| Device model, OS version, locale, timezone | Optional; disabled by default | Additional installation-level analytics | Initial `collectDeviceMetadata` setting plus `ShoparDiscovery.setOptionalDeviceMetadataCollectionEnabled` when host preference changes; `false` affects future requests, without an optional-data queue |

Discovery never requests or attaches an Agency PWT and never signs a consumer into Shopar. Its installation ID is a pseudonymous installation identifier; unauthenticated requests are not inherently unidentifiable. A displayed offer is opened by its exact backend reference. Raw affiliate targets are not returned in catalog payloads or SDK callbacks; the browser necessarily receives the destination after Shopar's short-link redirect.

## Storage and reset behavior

- Agency credentials are scoped by SDK type, gRPC host/port, and `app_id`, stored with platform-protected storage, and never written to logs or analytics.
- Discovery has no credential store and cannot read Agency token storage.
- Android installation IDs and Agency credentials use app-private no-backup storage. Reinstall or host-data clear creates a new installation ID and ends the local session. The host may call `ShoparDiscovery.resetInstallationIdentity()` only after an explicit user reset request; prior server events remain historical records.
- The SDK does not use advertising IDs, contacts, precise location, or hardware serial numbers.
- Existing Shopar retention, reward, fraud, correction, and reversal policies apply. SDK impressions and taps do not independently create a reward.

## Google Play Data safety guidance for host publishers

The host publisher must include data processed by embedded SDKs in its Play Console Data safety declaration. For Agency, disclose email, account/profile/social information, installation/device metadata, and app interactions used for account functionality and analytics. For Discovery, disclose the installation identifier, optional device attributes when enabled, and product-view/tap interactions used for analytics and attribution. Explain the collection in the host app's privacy policy and provide the appropriate user-facing notices. The SDK does not perform ad tracking or sell this data.

Apple SDK privacy manifests are included per module in `Sources/ShoparAgency/PrivacyInfo.xcprivacy` and `Sources/ShoparDiscovery/PrivacyInfo.xcprivacy`. Apple requires third-party SDKs to declare the data types they collect and their purpose; the declarations here reflect the module APIs and optional Discovery device fields.
