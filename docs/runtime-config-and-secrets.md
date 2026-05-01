# Runtime Config and Secrets

SoulMatch separates public mobile configuration from private server secrets.

## Safe to manage from Control Panel

These values are public client identifiers and may be changed without rebuilding the app:

- Google Web Client ID
- Razorpay Key ID
- Support email
- Login copy, legal copy, branding, preview image URL
- Feature flags, pricing labels, plan content

In the admin console, use:

`Dynamic Config > Public App Integrations`

The Android app reads these values from:

`GET /api/v1/public/config`

If a value is blank, the app falls back to the existing local `BuildConfig` value.

## Never ship these in the app

These are real secrets and must remain server-side:

- Google OAuth client secret
- Razorpay key secret
- Firebase service account private key
- JWT signing secrets
- Database passwords
- SMS/email provider secrets

Use environment variables, Docker secrets, or a cloud vault for these values. The control panel can reference whether a provider is configured, but it should not expose the raw secret to the Android app.
