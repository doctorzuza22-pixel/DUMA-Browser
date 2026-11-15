# DUMA Revenue Architecture

Status: architecture foundation approved; integrations are not yet live.

## Product decisions

- DUMA Plus: proposed Google Play auto-renewing subscription, R30 per month, with a 7-day introductory free trial configured in Play Console.
- Free learning and browsing remain available without payment.
- Plus entitlement removes DUMA-served advertising and unlocks the six approved Plus learning features.
- Revenue Centre is a private owner/admin area. It is not linked from public navigation and is never exposed to ordinary users.
- Reporting currency: ZAR (South African rand), while preserving the original currency and amount for any future international transaction.

## Revenue streams

1. **Subscriptions** — Google Play Billing for Android Play-distributed digital features. The app sends purchase tokens to a secure backend. The backend verifies each token with the Google Play Developer API before granting Plus. Store product ID proposal: `duma_plus_monthly`; create and configure it in Play Console before production.
2. **Advertising** — AdMob integration, with ad placements controlled by remote configuration and suppressed for verified Plus entitlements. Ads must not be shown in ways that interrupt lessons or imitate learning controls. Child-directed and mixed-audience treatment must be configured to match DUMA's declared Play target audience and applicable law. Until the audience classification and compliant ad treatment are confirmed, child-facing ad requests remain disabled.
3. **Affiliate referrals** — approved partner catalogue, tagged outbound links, click/referral identifiers, conversion imports or partner callbacks, commission rules, reversals and payout reconciliation. No commission is recorded as earned merely because a link was clicked.
4. **Sponsored placements** — paid placements for approved businesses, courses or education providers. Each placement has an advertiser, creative, audience/age suitability review, start/end dates, budget or fixed fee, disclosure label and approval status. Sponsored content is visibly labelled.
5. **Partnerships** — contracts, referral terms, invoices, partner statements, payment milestones and renewal dates. Manual entries are supported initially; verified integrations can be added later.
6. **Other income** — extensible category for grants, licensing, institutional access or future services, each recorded with source, evidence and approval.

## Security boundary

- Public app/WebView is an untrusted client. It must never contain admin credentials, payment provider secrets, service-account keys, commission rules that grant money, or authoritative entitlement flags.
- All money, subscription and admin operations go through authenticated backend APIs over TLS.
- Admin access requires server-side role checks on every request. Initial roles: `owner_admin` and `finance_admin`; default access is deny. Only the owner can invite or remove admins and change payout/bank details. Add MFA before enabling the Revenue Centre.
- Public users can see their own subscription state and clearly disclosed offers only. They cannot query aggregate revenue, partner settlements, other users, or admin endpoints.
- Keep an immutable audit trail for admin sign-ins, role changes, manual adjustments, approvals, exports and payout-detail changes.
- Store secrets only in the backend hosting provider's secret manager / GitHub Actions secrets where appropriate. Never commit credentials or ask users to paste private keys into chat.

## Core backend modules

- **Identity & roles:** account identity, admin role, MFA state, session revocation.
- **Entitlements:** user, product, purchase token reference, state, start/end timestamps, source, last verification time. Store token securely; enforce uniqueness and idempotency.
- **Billing adapters:** Google Play verification and lifecycle updates; future adapters can be added without changing the ledger.
- **Ad configuration:** placement definitions, consent/age-treatment settings, Plus ad suppression and remote kill switch.
- **Affiliate tracking:** partner, campaign, click ID, conversion, commission rate, status, reversal and payout batch.
- **Sponsored inventory:** advertiser, creative, disclosure, suitability review, schedule, invoice and delivery metrics.
- **Partnerships:** partner, agreement, referral/invoice records, due dates and settlement status.
- **Ledger & reconciliation:** append-only financial events; gross amount, fees, tax, net amount, currency, source reference, event date, settlement date and status. Corrections are reversal/adjustment entries, not destructive edits.
- **Reports:** daily/monthly totals by stream, gross vs net, fees, tax, pending/confirmed/reversed commissions, settlements, expenses and estimated operating profit.
- **Audit & exports:** admin activity log and CSV export with permission checks.

## Minimum data model

- `users`: user ID, account status, created timestamp.
- `admin_memberships`: user ID, role, invited by, status, MFA enabled.
- `subscriptions`: user ID, store, product ID, unique purchase-token hash/reference, state, period start/end, verification timestamp.
- `entitlements`: user ID, entitlement key, active flag, valid-until, source subscription.
- `partners`: partner ID, name, type, contract status, payment terms.
- `affiliate_clicks`: click ID, partner ID, campaign ID, timestamp, non-sensitive attribution fields.
- `affiliate_conversions`: partner conversion ID, click ID, gross sale, commission, currency, status, reversal reference.
- `sponsored_campaigns`: advertiser ID, placement, creative reference, disclosure, audience approval, schedule, price, status.
- `partnership_transactions`: partner ID, agreement reference, invoice/reference, amount, currency, due date, settlement status.
- `ledger_entries`: immutable entry ID, stream, event type, source reference, gross, fee, tax, net, currency, event time, settlement time, status, reversal link.
- `expenses`: category, supplier, reference, amount, currency, date, approval status.
- `admin_audit_log`: actor, action, target, timestamp, outcome, request reference.

Use opaque IDs. Avoid storing children's sensitive personal information or using children's activity for advertising profiles.

## Required event flows

### Google Play subscription
1. Android BillingClient launches the Play purchase flow.
2. App sends purchase token and signed-in account mapping to backend.
3. Backend verifies with Google Play Developer API; reject reused tokens and pending/unverified purchases.
4. Backend grants or updates Plus entitlement only after verification.
5. Google Play RTDN arrives through Cloud Pub/Sub; backend de-duplicates message IDs, fetches authoritative subscription state and updates entitlement.
6. Cancellation, grace period, hold, expiry, refund or revocation updates access and ledger/reconciliation records.
7. User can restore/query purchases and open Play's subscription management page.

### Advertising
1. Backend returns eligible placements/configuration.
2. App checks verified entitlement and age/consent treatment.
3. No ad request for Plus users; no child-facing ad request until policy-compliant treatment is configured.
4. Aggregate ad revenue is imported from AdMob reports; estimated earnings are clearly separated from finalised/payout amounts.

### Affiliate and sponsored revenue
1. Admin approves partner/campaign and commercial terms.
2. Outbound referral receives a non-identifying campaign/click reference.
3. Partner callback or statement supplies a conversion.
4. Conversion remains pending until validated; reversals/returns reduce commission.
5. Confirmed amounts enter the ledger and payout reconciliation.

## Revenue Centre screens (admin only)

- Overview: gross revenue, net revenue, fees, tax, expenses, estimated profit, pending settlements.
- Revenue streams: subscriptions, ads, affiliates, sponsored placements, partnerships, other.
- Subscriptions: active/trial/cancelled/expired counts and reconciliation status; never expose raw purchase tokens.
- Affiliates: partner performance, clicks, confirmed conversions, reversals and payable commissions.
- Sponsors: campaigns, approvals, delivery, invoices and balances.
- Partners: contracts, statements and payment history.
- Finance: ledger, expenses, payout batches, exports and reconciliation.
- Admin & audit: staff access, MFA, security events and change history.

## API boundary (illustrative contract)

- `POST /billing/google-play/verify`
- `POST /billing/google-play/rtdn`
- `GET /me/entitlements`
- `GET /admin/revenue/summary`
- `GET /admin/revenue/streams`
- `GET /admin/ledger`
- `POST /admin/ledger/adjustments` (creates audited adjustment/reversal only)
- `POST /admin/partners`
- `POST /admin/affiliate/conversions/import`
- `POST /admin/sponsored-campaigns`
- `POST /admin/expenses`
- `GET /admin/reports/export`

All `/admin/*` routes require server-side admin authorization and audit logging. These route names describe the intended contract; they are not implemented endpoints yet.

## Rollout gates

1. Choose and configure backend hosting, database, authentication and secret storage.
2. Create/verify Play Console developer account and app listing; configure subscription product and 7-day trial.
3. Implement and test server-side purchase verification, RTDN and entitlement lifecycle.
4. Confirm DUMA's declared target audience and legal/privacy requirements before enabling ads; implement compliant age treatment and ad SDK configuration.
5. Sign affiliate/advertiser/partner agreements before importing real conversions or accepting sponsorship money.
6. Build the private Revenue Centre only after backend authentication and authorization are in place.
7. Test security, duplicate events, refunds, cancellations, reversals, reconciliation and exports before launch.

## Current implementation status

This document establishes the agreed architecture only. The current repository is a native Android Activity hosting local HTML assets; it does not yet contain the secure backend, database, admin authentication, Google Play Billing integration, AdMob SDK, affiliate callbacks, sponsor management or Revenue Centre UI. No revenue is being collected by this commit.
