# Governance

`cloud-itonami-isco-9331` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- HandPedalDispatchGovernor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval — in
  particular, a proposal that would finalize a route/traffic-navigation
  decision or override a driver's on-road safety judgment is always a
  permanent block, never an escalation.
- every commit, hold and approval path is auditable.
- real driver/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling driver/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to grant this actor route/traffic-navigation finalization
  authority
