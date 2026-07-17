# cloud-itonami-isco-9331

Open Occupation Blueprint for **ISCO-08 9331**: Hand and Pedal Vehicle Drivers.

This repository designs a forkable OSS business for a hand/pedal-vehicle
dispatch coordination practice: a dispatch/logistics coordination robot
manages driver-roster scheduling, trip/fare/incident-report logging,
safety-concern surfacing and maintenance-order coordination for
rickshaw/pedicab/hand-cart operations under a governor-gated actor, so the
practice keeps its own operating records instead of renting a closed
dispatch SaaS — and never takes on-road navigation authority away from the
human driver.

**Maturity: `:implemented`.** `src/handpedaldispatch/` implements the
`HandPedalDispatchActor` as a `langgraph.graph/state-graph`
(`handpedaldispatch.actor`) wired to a `Hand/Pedal Vehicle Dispatch Advisor`
(`handpedaldispatch.advisor`) and an independent `HandPedalDispatchGovernor`
(`handpedaldispatch.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. HARD invariants (always hold, never overridable):
driver provenance (an operating permit must be independently verified and
registered), no-actuation (`:effect` must be `:propose`), a closed
op-allowlist (`:log-trip-record`, `:schedule-dispatch-operation`,
`:flag-safety-concern`, `:coordinate-maintenance-order` — nothing else is
in scope), a registered vehicle basis for any maintenance-order proposal,
and — the safety-critical invariant for this occupation — any proposal
that would finalize a route/traffic-navigation decision or override the
driver's on-road safety judgment is a hard, **permanent** block that no
human escalation can override. Always-escalate cases (human sign-off
regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:flag-safety-concern`
(always) and a `:coordinate-maintenance-order` whose estimated cost exceeds
the vehicle's registered cost ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a dispatch/logistics coordination
robot performs driver-roster scheduling, trip/fare/incident-report logging
and maintenance-order coordination for hand/pedal-vehicle operations under
an actor that proposes actions and an independent **Hand/Pedal Dispatch
Governor** that gates them. The governor never operates a vehicle itself
and never dispatches hardware; `:high`/`:safety-critical` actions (such as
a safety-concern flag or an over-ceiling maintenance order) require human
sign-off. Route/traffic-navigation finalization and overriding the
driver's on-road safety judgment are outside this actor's authority
entirely — that authority never leaves the human driver, and no proposal
of that shape is ever auto-commit-eligible or escalation-overridable.

This actor coordinates **DISPATCH/LOGISTICS SCHEDULING ONLY** — it never
operates the vehicle.

## Core Contract

```text
driver roster + vehicle registry + trip/fare/incident data + maintenance need
        |
        v
Hand/Pedal Vehicle Dispatch Advisor -> HandPedalDispatchGovernor -> commit dispatch/logistics record, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, finalize a route/traffic-navigation decision,
override the driver's on-road safety judgment, or disclose sensitive data
without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `9331`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
