# SoulMatch API Testing and Load Testing

Generated from the current backend routes and Android API client.

## Files

| File | Purpose |
| --- | --- |
| `soulmatch.postman_collection.json` | Functional API collection for Postman/Newman |
| `soulmatch.postman_environment.json` | Environment variables with safe placeholders |
| `k6/soulmatch-smoke-load.js` | Read-heavy k6 smoke/load script |

## Import in Postman

1. Open Postman.
2. Click `Import`.
3. Import `api-testing/soulmatch.postman_collection.json`.
4. Import `api-testing/soulmatch.postman_environment.json`.
5. Select environment `SoulMatch Production Test Environment`.
6. Set `phone`, `otp`, `access_token`, or admin values as needed.

## Functional Test Flow

Recommended order:

1. `00 Public and Health -> Runtime Config`
2. `01 Auth -> Verify OTP` or paste a known `access_token`
3. `02 Profile -> My Profile`
4. `03 Matches and Search -> Recommended Matches`
5. `03 Matches and Search -> Basic Search`
6. `04 Interests -> Send Interest`
7. `06 Notifications -> Notifications`
8. `07 Payments -> Plans`
9. `07 Payments -> Create Order`

## Load Testing

Install k6:

Windows:

```powershell
winget install k6.k6
```

Run a safe public-only load test:

```powershell
k6 run api-testing/k6/soulmatch-smoke-load.js
```

Run authenticated load test:

```powershell
$env:BASE_URL="http://20.204.142.19/api/v1"
$env:ACCESS_TOKEN="PASTE_USER_ACCESS_TOKEN"
$env:VUS="10"
$env:DURATION="2m"
k6 run api-testing/k6/soulmatch-smoke-load.js
```

Important:

- Do not load test `/auth/send-otp` against Firebase or SMS providers. It can cost money and trigger abuse limits.
- Do not load test payment create/verify endpoints on production without a separate Razorpay test plan.
- Start with 5 to 10 virtual users on the current Azure VM. The VM is small and can be overloaded quickly.
- For production readiness testing, use a staging VM and seeded test accounts.

## Newman CLI

Install:

```powershell
npm install -g newman
```

Run collection:

```powershell
newman run api-testing/soulmatch.postman_collection.json -e api-testing/soulmatch.postman_environment.json
```

For write-heavy folders, run manually and carefully. The collection includes destructive/admin actions for completeness.
