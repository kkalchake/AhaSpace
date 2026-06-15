# Password Reset Feature

**Status:** Ready for Development  
**Target Launch:** May 26, 2026  
**Sprint 1:** Apr 28 - May 9 (Backend)  
**Sprint 2:** May 12 - May 23 (Frontend)  

---

## Problem & Solution

### Why
15% of users abandon after 2 failed logins. Support handles 45 password tickets/month manually.

### What
Self-service password reset via secure email tokens:
1. User requests reset by username
2. System sends 256-bit random token (1-hour expiry, single-use)  
3. User clicks link, sets new password
4. System invalidates old password

### Success
| Metric | Target | Measurement |
|--------|--------|-------------|
| Completion rate | >85% | `password_reset.completed` event |
| End-to-end time | <3 min | Timer from request → completion |
| Support tickets | <5/month | Zendesk tag analysis |
| Token abuse | <0.1% | Failed validations per IP |

---

## Acceptance Criteria

**AC1 - Request Reset**
- POST `/api/auth/forgot-password` with username
- Always returns 202 (no user enumeration)
- Rate limit: 3/hour per username

**AC2 - Email Delivery**
- Reset link: `https://ahaspace.com/reset-password?token={token}`
- Expires in 1 hour
- HTTPS only, no PII in URL

**AC3 - Password Requirements**
- Min 8 chars, 1 uppercase, 1 lowercase, 1 digit
- Cannot reuse last 3 passwords (v1.1)

**AC4 - Token Security**
- 256-bit cryptographically random
- Single-use (deleted after consumption)
- 1-hour expiry, no extensions

**AC5 - Completion**
- Success message: "Password reset successfully"
- Redirect to login with `?reset=success`
- Old password immediately invalid

---

## Architecture

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│  React Frontend │──────▶  Spring Boot API │──────▶  PostgreSQL      │
│  /forgot-password     │      │  • Token Service      │      │  • users        │
│  /reset-password      │      │  • Rate Limit (Redis) │      │  • password_reset_tokens │
└─────────────────┘      │  • Email Service      │      └─────────────────┘
                         └──────────────────┘
                                  │
                         ┌────────┴────────┐
                    SendGrid              Redis
```

### Data Model

```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,      -- SHA-256 for lookup
    token_encrypted BYTEA NOT NULL,              -- AES-256-GCM for audit
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_timestamp TIMESTAMPTZ NOT NULL,
    used_timestamp TIMESTAMPTZ,                  -- NULL until consumed
    ip_address INET,                             -- Security audit
    user_agent_hash VARCHAR(64)
);

CREATE INDEX idx_token_hash ON password_reset_tokens(token_hash);
CREATE INDEX idx_expiry ON password_reset_tokens(expiry_timestamp) WHERE used_timestamp IS NULL;
```

---

## API Specification

### POST /api/auth/forgot-password
```yaml
Request:
  username: string (3-50 chars, alphanumeric)
  
Response 202:
  message: "If an account exists, a reset email has been sent"
  
Response 429:
  error: "Too many attempts. Retry in {minutes} minutes"
  retryAfter: 2700
```

### GET /api/auth/verify-token
```yaml
Request:
  token: query param (43 chars, Base64url)
  
Response 200:
  valid: true
  username: "john_doe"
  emailMasked: "j***@example.com"
  expiresInMinutes: 45
  
Response 401:
  error: "Invalid or used token"
  
Response 410:
  error: "Token expired"
```

### POST /api/auth/reset-password
```yaml
Request:
  token: string (43 chars)
  newPassword: string (8+ chars, complexity required)
  
Response 200:
  success: true
  message: "Password reset successfully"
  redirectUrl: "/login?reset=success"
  
Response 400:
  errors: {newPassword: "Password must contain 1 uppercase, 1 lowercase, 1 digit"}
  
Response 409:
  error: "Cannot reuse recent passwords"
```

---

## Security Requirements

| Threat | Mitigation |
|--------|-----------|
| User enumeration | Always return 202, constant response time |
| Token brute force | 256-bit entropy (>10^57 years to guess) |
| Token replay | Single-use flag, atomic DB update |
| Email interception | HTTPS links, 1-hour expiry |
| Rate limit bypass | Per-IP + per-username limits (Redis) |
| Timing attacks | Response times within 5% variance |

---

## SLOs & Monitoring

| SLI | Target | Alert |
|-----|--------|-------|
| Availability | 99.9% | <99.9% for 5m → Page |
| Latency p99 | <500ms | >1s for 10m → Warning |
| Error rate | <1% | >1% for 5m → Warning |
| Token reuse attempts | 0 | >10/hour → Critical |

**Metrics:**
- `password_reset_requested_total` (counter)
- `password_reset_completed_total` (counter with outcome label)
- `password_reset_duration_seconds` (histogram)

---

## Work Items

### Sprint 1: Backend (Apr 28 - May 9)

| # | Task | Assignee | Points | Depends On |
|---|------|----------|--------|------------|
| 1 | Database migration: `password_reset_tokens` table | Backend | 3 | - |
| 2 | `PasswordResetToken` JPA entity + repository | Backend | 2 | #1 |
| 3 | `TokenGenerator` service (256-bit secure random) | Backend | 3 | - |
| 4 | `RateLimitService` (Redis-backed) | Backend | 5 | - |
| 5 | `EmailService` + SendGrid integration | Backend | 5 | - |
| 6 | `POST /forgot-password` endpoint | Backend | 5 | #2, #3, #4, #5 |
| 7 | `GET /verify-token` endpoint | Backend | 3 | #2, #4 |
| 8 | `POST /reset-password` endpoint | Backend | 5 | #2, #3, #4 |
| 9 | Backend unit & integration tests (>80% coverage) | Backend | 5 | #6, #7, #8 |
| 10 | DevOps: Redis, SendGrid credentials, feature flags | DevOps | 3 | - |

**Sprint 1 Total:** 39 points

### Sprint 2: Frontend & Launch (May 12 - May 23)

| # | Task | Assignee | Points | Depends On |
|---|------|----------|--------|------------|
| 11 | `ForgotPassword.jsx` page + form validation | Frontend | 3 | #6 |
| 12 | `ResetPassword.jsx` page + token handling | Frontend | 5 | #7, #8 |
| 13 | Login page updates (forgot link, success banner) | Frontend | 2 | - |
| 14 | E2E tests (Playwright) | QA | 5 | #11, #12 |
| 15 | Prometheus metrics + Grafana dashboard | DevOps | 3 | #6, #7, #8 |
| 16 | Load testing (10x traffic) | QA/SRE | 3 | #10 |
| 17 | Security review + penetration test | Security | 3 | #6, #7, #8 |
| 18 | Privacy Impact Assessment | Privacy | 2 | - |
| 19 | Documentation (API docs, runbooks) | Docs | 2 | - |
| 20 | Launch Readiness Review | EM/PM | 1 | All above |

**Sprint 2 Total:** 29 points

---

## Launch Checklist

**Must-Have (Blocking):**
- [ ] Security review: No critical/high findings
- [ ] PIA signed by DPO
- [ ] Load testing passed (10x traffic)
- [ ] Test coverage >80%
- [ ] CI/CD green for 3 consecutive runs
- [ ] Monitoring dashboards live
- [ ] Feature flag configured
- [ ] On-call runbook ready

**Rollout:**
1. Day 1 (May 26): Internal users only
2. Day 2: 10% of users
3. Day 3-5: 50% if healthy
4. Day 6+: 100%

**Rollback:** Set feature flag `password-reset` → `disabled` (instant)

---

## Runbook: Password Reset Alert

**Alert:** `HighPasswordResetFailureRate` (>10% failure for 5m)

1. Acknowledge in PagerDuty (5 min SLA)
2. Check dashboard: `grafana.ahaspace.com/d/password-reset`
3. Identify failure type:
   - 401 errors → Check DB connection
   - 429 errors → Possible abuse, verify rate limits
   - 5xx errors → Check logs, dependencies
4. Mitigation:
   - Increase rate limits: `RATE_LIMIT_FORGOT_PASSWORD=5`
   - Disable feature flag if critical
5. Escalate if unresolved in 30 minutes

---

## Compliance

**GDPR:**
- Lawful basis: Legitimate Interest (Art. 6(1)(f))
- Data retention: 30 days for expired tokens
- User deletion cascades to `password_reset_tokens`

**Email (CAN-SPAM/CASL):**
- From: `security@ahaspace.com`
- Transactional (no unsubscribe required)
- SPF/DKIM/DMARC configured

**Audit Trail:**
```json
{
  "event": "PASSWORD_RESET_COMPLETED",
  "timestamp": "2026-04-24T10:30:00Z",
  "userIdHash": "sha256:abc123...",
  "tokenId": "tok_12345",
  "success": true
}
```
Retention: 1 year

---

## Dependencies

**Hard:**
- Redis 6.x (rate limiting)
- SendGrid account (email delivery)
- `users` table with `findByUsername()`

**Soft:**
- LaunchDarkly (feature flags, recommended)

---

## Questions & Decisions

| Question | Decision | Date |
|----------|----------|------|
| Email provider | SendGrid (existing account) | Apr 30 |
| Token expiry | 1 hour | Apr 28 |
| Password history | 3 passwords (v1.1) | May 5 |
| Rate limit window | 1 hour | May 5 |

---

**Document Owner:** Engineering + Product  
**Last Updated:** April 24, 2026  
**Next Review:** Weekly during sprint
