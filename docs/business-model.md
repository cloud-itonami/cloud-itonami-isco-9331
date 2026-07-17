# Business Model: Hand/Pedal Vehicle Dispatch Coordination Practice

## Classification

- Repository: `cloud-itonami-isco-9331`
- ISCO-08: `9331`
- Occupation: Hand and Pedal Vehicle Drivers
- Social impact: informal-worker-livelihoods, urban-mobility-access, road-safety

## Customer

- rickshaw / pedicab / hand-cart driver cooperatives
- independent hand/pedal-vehicle operators

## Offer

- trip/fare/incident-report logging
- driver-roster and dispatch scheduling coordination
- safety-concern intake and routing to human review
- vehicle maintenance-order coordination

## Revenue

- per-driver monthly coordination fee
- per-trip logging fee

## Trust Controls

- no dispatch/logistics record commits without a driver whose operating
  permit is independently verified and registered
- closed op-allowlist: only trip logging, dispatch scheduling, safety-concern
  flagging and maintenance-order coordination are in scope — nothing else
- any proposal that would finalize a route/traffic-navigation decision or
  override the driver's on-road safety judgment is rejected as a permanent,
  non-overridable block — this actor never takes on-road authority away
  from the human driver
- safety-concern flags always route to human review, regardless of
  confidence
- maintenance orders above the vehicle's registered cost ceiling always
  route to human sign-off
- dispatch and logging records are auditable, not editable
