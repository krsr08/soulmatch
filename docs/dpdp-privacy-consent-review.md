# DPDP Privacy Consent Review

Last updated: 10 May 2026

## Scope

This review covers the highest-risk SoulMatch data flows:

- profile photos
- member KYC / verification documents
- agent KYC documents
- agent sharing from SoulMatch Assistance
- SoulMatch Assistance opt-in and withdrawal

## Legal Reference

The review is aligned to India's DPDP framework as notified by the Government of India, including consent transparency, purpose limitation, data minimisation, storage limitation, security safeguards, accountability, consent withdrawal, and breach-notification expectations.

- PIB DPDP Rules 2025 backgrounder: https://www.pib.gov.in/PressReleasePage.aspx?PRID=2190655
- MeitY full DPDP Rules reference: https://www.meity.gov.in/static/uploads/2025/11/53450e6e5dc0bfa85ebd78686cadad39.pdf

## Implementation Status

| Data Flow | Purpose Shown / Policy Updated | Consent Ledger | Withdrawal / Change Path | Admin Audit |
|---|---|---|---|---|
| Profile photos | Yes | `photo_upload` | Delete photos / change photo privacy | user change audit |
| Member verification / KYC | Yes | `kyc_upload` | Support/admin review path | verification/admin alerts |
| Agent KYC | Yes | `agent_kyc_upload` | Re-upload after rejection | advisor KYC admin review |
| SoulMatch Assistance | Yes | `soulmatch_assistance` | Toggle off in profile | user change audit |
| Selected agent sharing | Yes | `agent_profile_share` | Remove selection or disable assistance | assisted assignment events |

## Notice Version

Current notice version: `dpdp-2026-05-10-v1`

The runtime privacy policy was updated on 10 May 2026 to explicitly explain:

- why photos and KYC documents are collected
- who can access verification documents
- that SoulMatch Assistance is optional
- that selected agents may receive profile context for direct offline support
- that SoulMatch does not participate in offline agent conversations
- that users can withdraw optional agent sharing

## Data Minimization Rules

- Do not show KYC files to normal members.
- Admin/service access to documents is limited to verification roles.
- Agent sharing is selection-based; only active selected agents receive profile visibility through the agent profile list.
- Crashlytics breadcrumbs must not include mobile numbers, email addresses, OTPs, KYC IDs, payment IDs, or raw API payloads.
- Security reports must redact findings and never print secret values.

## Verification Queries

```sql
SELECT consent_type, status, COUNT(*)
FROM consent_events
GROUP BY consent_type, status
ORDER BY consent_type, status;
```

```sql
SELECT profile_id, consent_type, status, notice_version, created_at
FROM consent_events
WHERE consent_type IN ('agent_profile_share', 'soulmatch_assistance')
ORDER BY created_at DESC
LIMIT 50;
```

```sql
SELECT advisor_id, document_type, status, consent_event_id
FROM advisor_kyc_documents
ORDER BY uploaded_at DESC
LIMIT 50;
```

## Remaining Manual Controls

- Confirm Google/Firebase API key restrictions in Google Cloud Console.
- Confirm Razorpay, Twilio, Azure, Firebase service account, and deployment token rotation logs after any exposed secret finding.
- Confirm production backups are uploaded off-VM and restore-tested.
- Confirm privacy/support contact details are correct before Play Store listing.
