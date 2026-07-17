# Contributing

`cloud-itonami-isco-9331` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:dev:test
clojure -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real driver data, credentials or operating documents.
- Keep production writes and disclosures behind HandPedalDispatchGovernor.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Never add an op, field or code path that would let this actor finalize a
  route/traffic-navigation decision or override a driver's on-road safety
  judgment — that authority never leaves the human driver, and the closed
  op-allowlist / scope-exclusion invariants exist specifically to prevent
  this.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
