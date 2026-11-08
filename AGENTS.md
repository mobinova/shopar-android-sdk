# Shopar Android SDK Guidelines

- Keep the Agency and Discovery Gradle products independently consumable.
- Never add feature flags, default-disabled activation, staged rollout switches, or a global kill switch.
- Use the public, allowlisted protocol bindings checked into this repository; builds must not fetch private Shopar repositories or credentials.
- Agency sessions use only the capability-limited Agency contract. Discovery requests never attach a PWT, even if both products are installed in one host app.
- Use standard Android app-private storage/OS protections. Do not add SDK-managed encryption or token wrapping.
- Never return PWTs/refresh tokens through public APIs/callbacks or write credentials to logs, analytics, crash reports, or deep links.
- Keep `app_id`, `placement_id`, session source, link source, and financial owner as distinct values.
- Do not expose raw affiliate targets in catalog, placement, or SDK callbacks; use Shopar redirect handoff.
- Do not collect advertising IDs, hardware serials, MAC addresses, or fingerprinting data.
- Raster assets use WebP where supported. Keep the public checkout buildable without internal credentials.
