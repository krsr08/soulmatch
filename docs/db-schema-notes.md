# SoulMatch DB Schema Notes

Last updated: 2026-05-03

Purpose: keep a simple, human-readable view of the important SoulMatch database areas and the latest market-readiness schema changes.

## Main Databases

| Database | Used For |
|---|---|
| PostgreSQL | Users, profiles, preferences, interests, verification, notifications, payments, advisor operations, audit events |
| MongoDB | Chat conversations and messages |
| Redis | OTP/session/cache support |

## Core PostgreSQL Areas

| Area | Main Tables |
|---|---|
| Identity and access | `users`, refresh/session-related auth data |
| Profile | `profiles`, `profile_preferences`, `profile_photos`, `profile_privacy_settings` |
| Discovery | `interests`, `favorites`, `profile_views`, `blocks`, `reports` |
| Verification and trust | `profile_verification_requests`, verification-related proof rows, `audit_logs` |
| Family workflows | `family_match_decisions`, `family_match_decision_comments` |
| Notifications | `notifications`, `device_tokens` where enabled |
| Payments and plans | `transactions`, `payment_orders`, `app_config` plan/feature-gate entries |
| Advisor assisted matching | `advisors`, `advisor_service_areas`, `assisted_match_profiles`, `assisted_match_assignment_events` |
| Analytics and audit | `analytics_events`, `audit_logs` |

## Migration 010: Market Readiness v2

File: `database/migrations/010_market_readiness_v2.sql`

This migration adds the schema support required for the new market-readiness features:

- Adds `family_vote` to `family_match_decisions`.
- Creates `family_match_decision_comments` for family notes and decision discussion.
- Creates `chat_message_reports` for safer communication and moderation.
- Adds indexes for family comments, chat reports, and family vote/status queries.
- Updates `app_config.trust_engine` to version 2 so trust scoring is explicit and explainable.
- Updates `app_config.monetization.featureGates` with:
  - `verified_plus`
  - `family_assist`
  - `advisor_assisted`
  - `spotlight`
  - `contact_unlock`

## Current Schema Caveats

- PostgreSQL is still shared across services. This is acceptable for the current VM stage, but service-owned schemas or service-owned databases should be introduced before large-scale growth.
- MongoDB chat storage is not yet backed by full retention, export, deletion, and moderation evidence workflows.
- Notification records exist, but retry/dead-letter delivery tables and provider audit dashboards still need a worker-driven implementation.
- Payment tables exist, but renewals, refunds, invoices, and full entitlement enforcement still need deeper lifecycle logic.
