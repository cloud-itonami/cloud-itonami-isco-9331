# Operator Guide

## First Deployment

1. Define the operator's service area and driver-onboarding/permit
   verification process.
2. Define consent and purpose categories for trip/fare data.
3. Run synthetic dispatch/logging cases.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions
   (safety-concern flags, over-ceiling maintenance orders).
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- driver operating-permit independent verification and registration
- consent and disclosure log
- safety-critical escalation path (safety-concern flags always escalate)
- provenance for all operating records
- human review for high-risk cases
- audit export for all gated actions

## Scope Boundary

This actor coordinates dispatch/logistics scheduling only. It never
operates a rickshaw/pedicab/hand-cart, and it never finalizes a
route/traffic-navigation decision or overrides a driver's on-road safety
judgment — operators must not configure or extend this actor to attempt
either; the governor treats such proposals as a permanent, non-overridable
block by design.

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans, and that no configuration grants this actor route/traffic-navigation
finalization authority.
