# Security Policy

This project handles hand and pedal vehicle driver dispatch/logistics
operating workflows. Treat vulnerabilities as potentially high impact even
when the demo data is synthetic — this occupation has real physical-safety
stakes (traffic collision, driver fatigue).

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real driver or operator data exposure
- authorization bypass
- HandPedalDispatchGovernor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path by which this actor could finalize a route/traffic-navigation
  decision or override a driver's on-road safety judgment

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
gftdcojp organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on driver data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real driver/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
