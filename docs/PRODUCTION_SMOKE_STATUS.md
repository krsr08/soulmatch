# Production Smoke Status

Last checked: 2026-05-18, after merge commit `9ae2b40`.

## Current Result

| Check | Result | Notes |
| --- | --- | --- |
| GitHub Production Deploy | Passed | Workflow run `26045282972` completed successfully. |
| GitHub Secret Scan | Passed | Push scan completed successfully. |
| Public API reachability from local machine | Blocked | `20.204.142.19:80` timed out. |
| SSH reachability from local machine | Blocked | `20.204.142.19:22` timed out. |
| Azure VM power state | VM deallocated | Confirmed by Azure CLI. |
| Public IP | Static `20.204.142.19` | IP is unchanged. |
| NSG rules | 22, 80, 443 allowed | Network rules are present; VM is off. |

## Interpretation

The deployment workflow passed because it was able to complete its configured deployment and internal checks during the run. Afterward, the VM is currently deallocated, so public smoke tests cannot reach the app. This is expected if the cost-control routine stops/deallocates the VM.

## Safe Smoke Test When VM Is Started

Run these checks only after intentionally starting the VM:

```powershell
Test-NetConnection 20.204.142.19 -Port 80
Test-NetConnection 20.204.142.19 -Port 22

$env:ADMIN_BASE_URL="http://20.204.142.19"
$env:AUTH_BASE_URL="http://20.204.142.19"
$env:PROFILE_BASE_URL="http://20.204.142.19"
$env:SEARCH_BASE_URL="http://20.204.142.19"
$env:CHAT_BASE_URL="http://20.204.142.19"
$env:NOTIFICATION_BASE_URL="http://20.204.142.19"
$env:PAYMENT_BASE_URL="http://20.204.142.19"
node test_folder/api-smoke.js
```

Do not run OTP smoke scripts against production unless mock OTP is intentionally enabled or Twilio is fully configured, because those tests create temporary users.

