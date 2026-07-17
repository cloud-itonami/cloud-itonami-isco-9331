(ns handpedaldispatch.governor
  "HandPedalDispatchGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every dispatch/logistics coordination proposal an advisor may make
  for a hand/pedal-vehicle driver (rickshaw/pedicab/hand-cart). The
  governor never operates a vehicle itself, never finalizes a
  route/traffic-navigation decision and never overrides a driver's
  on-road safety judgment — that authority never leaves the human
  driver. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor.

  This actor coordinates DISPATCH/LOGISTICS SCHEDULING ONLY.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. driver provenance        — the driver's operating permit must
                                  be independently verified and
                                  registered before any action.
    2. no-actuation              — proposal :effect must be :propose
                                  (the governor never operates a
                                  vehicle itself and never dispatches
                                  hardware; it only gates what the
                                  advisor may coordinate).
    3. closed op-allowlist       — only :log-trip-record,
                                  :schedule-dispatch-operation,
                                  :flag-safety-concern and
                                  :coordinate-maintenance-order may be
                                  proposed; any other op is out of
                                  scope for this actor.
    4. route/traffic-navigation scope exclusion — any proposal that
                                  would finalize a route/traffic-
                                  navigation decision or override the
                                  driver's on-road safety judgment is a
                                  hard, permanent block. This is never
                                  auto-commit-eligible and never
                                  overridable by human escalation —
                                  on-road navigation authority never
                                  leaves the human driver.
    5. vehicle basis              — a maintenance-order proposal must
                                  cite a REGISTERED vehicle belonging
                                  to this driver.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off — see
  docs/business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :flag-safety-concern (a vehicle-defect / road-hazard /
                                  driver-fatigue concern always
                                  requires human review — surfacing a
                                  safety concern is never
                                  auto-resolved).
    7. maintenance-order cost above ceiling (a :coordinate-maintenance-order
                                  proposal whose :estimated-cost
                                  exceeds the vehicle's registered
                                  :maintenance-cost-ceiling requires
                                  human sign-off — a cost overrun is a
                                  business decision, not a safety
                                  violation, so it escalates rather
                                  than hard-blocks).
    8. low confidence (< `confidence-floor`).

  Known self-tripping hazard (avoided by design): scope-exclusion
  terms below are phrased as finalization/execution ACTIONS ('override
  the driver's route judgment', 'finalize the traffic-navigation
  decision'), never as bare nouns ('route', 'traffic') — a bare-noun
  term list would false-positive on the mock advisor's own default
  rationale text (e.g. a :schedule-dispatch-operation proposal
  legitimately carries a :route-assignment field / mentions a route
  label without attempting to finalize navigation). See
  `handpedaldispatch.governor-test/default-mock-advisor-proposals-never-self-trip`."
  (:require [clojure.string :as str]
            [handpedaldispatch.store :as store]))

(def confidence-floor 0.6)

(def closed-op-allowlist
  #{:log-trip-record :schedule-dispatch-operation :flag-safety-concern
    :coordinate-maintenance-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Phrased as finalization/execution actions, never bare nouns — see
;; the "Known self-tripping hazard" docstring section above.
(def ^:private scope-exclusion-phrases
  ["override the driver's route judgment"
   "override the driver's on-road safety judgment"
   "override driver's on-road safety judgment"
   "finalize the traffic-navigation decision"
   "finalize the route decision"
   "directly control vehicle navigation"])

(defn- scope-violation?
  "True when any string field on `proposal` contains a
  scope-exclusion phrase — i.e. the proposal attempts to finalize a
  route/traffic-navigation decision or override the driver's on-road
  safety judgment. Hard, permanent block regardless of :op."
  [proposal]
  (let [text (str/lower-case
              (str/join " " (keep #(when (string? %) %) (vals proposal))))]
    (boolean (some #(str/includes? text (str/lower-case %)) scope-exclusion-phrases))))

(defn- hard-violations [{:keys [request proposal]} driver-record v]
  (let [{:keys [op estimated-cost]} proposal
        maint? (= :coordinate-maintenance-order op)]
    (cond-> []
      (nil? driver-record)
      (conj {:rule :no-driver :detail "未登録 driver（operating permit 未登録）"})

      (and driver-record (not (:permit-verified? driver-record)))
      (conj {:rule :unverified-permit
             :detail "operating permit が独立検証されていない driver での操作は不可"})

      (not (contains? closed-op-allowlist op))
      (conj {:rule :op-not-allowlisted
             :detail (str "closed op-allowlist に無い op: " (pr-str op))})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor はどんな操作も直接実行しない）"})

      (scope-violation? proposal)
      (conj {:rule :route-navigation-override
             :detail "route/traffic-navigation の確定または driver の路上安全判断の上書きは恒久的に禁止（human escalation でも解除不可）"})

      (and maint? (nil? v))
      (conj {:rule :unknown-vehicle :detail "未登録 vehicle への maintenance 提案は不可"})

      (and maint? v (not= (:driver-id v) (:driver-id request)))
      (conj {:rule :vehicle-wrong-driver :detail "vehicle が別 driver のもの"})

      (and maint? (some? estimated-cost) (not (number? estimated-cost)))
      (conj {:rule :invalid-cost :detail "estimated-cost が数値でない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `handpedaldispatch.store/Store`. Pure — never
  mutates the store, never operates a vehicle, never finalizes a
  route/traffic-navigation decision."
  [request context proposal store]
  (let [driver-record (store/driver store (:driver-id request))
        v (some->> (:vehicle-id proposal) (store/vehicle store))
        hard (hard-violations {:request request :proposal proposal} driver-record v)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        maint? (= :coordinate-maintenance-order (:op proposal))
        cost (:estimated-cost proposal)
        ceiling (:maintenance-cost-ceiling v)
        over-ceiling? (and maint? v (number? cost) (number? ceiling) (> cost ceiling))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-ceiling?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-ceiling?))}))
