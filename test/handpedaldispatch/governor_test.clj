(ns handpedaldispatch.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [handpedaldispatch.store :as store]
            [handpedaldispatch.advisor :as advisor]
            [handpedaldispatch.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-driver! st {:driver-id "driver-1" :name "Kobo Rickshaw"
                                 :permit-verified? true})
    (store/register-vehicle! st {:vehicle-id "V-1" :driver-id "driver-1"
                                  :name "cart-042"
                                  :maintenance-cost-ceiling 500})
    st))

(defn- trip-op []
  {:op :log-trip-record :effect :propose :driver-id "driver-1" :vehicle-id "V-1"
   :trip-id "T-1" :fare-amount 12 :confidence 0.9 :stake :low
   :rationale "proposed log-trip-record for driver driver-1"})

(defn- maint-op [cost]
  {:op :coordinate-maintenance-order :effect :propose :driver-id "driver-1"
   :vehicle-id "V-1" :estimated-cost cost :maintenance-type "brake-repair"
   :confidence 0.9 :stake :low
   :rationale "proposed coordinate-maintenance-order for driver driver-1"})

(def ^:private req {:driver-id "driver-1"})

(deftest ok-on-registered-driver-and-vehicle
  (let [st (fresh-store)
        v (governor/check req {} (trip-op) st)]
    (is (:ok? v))))

(deftest ok-at-exact-maintenance-ceiling-boundary
  (testing "the maintenance-cost ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (maint-op 500) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-driver
  (let [st (fresh-store)
        v (governor/check {:driver-id "nobody"} {} (trip-op) st)]
    (is (:hard? v))
    (is (some #(= :no-driver (:rule %)) (:violations v)))))

(deftest hard-on-unverified-permit
  (testing "a driver record that exists but whose permit was never independently verified is still a hard block"
    (let [st (store/mem-store)]
      (store/register-driver! st {:driver-id "driver-2" :name "New Driver"
                                   :permit-verified? false})
      (let [v (governor/check {:driver-id "driver-2"} {} (assoc (trip-op) :driver-id "driver-2") st)]
        (is (:hard? v))
        (is (some #(= :unverified-permit (:rule %)) (:violations v)))))))

(deftest hard-on-op-not-allowlisted
  (testing "closed op-allowlist enforced — anything outside the 4 registered ops is out of scope"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (trip-op) :op :dispatch-a-driver-directly) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowlisted (:rule %)) (:violations v))))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (trip-op) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-route-navigation-override-attempt
  (testing "a proposal that would finalize a route/traffic-navigation decision is a hard, permanent block regardless of :op or confidence"
    (let [st (fresh-store)
          v (governor/check req {}
                             (assoc (trip-op) :confidence 0.99
                                    :rationale "attempted to override the driver's route judgment")
                             st)]
      (is (:hard? v))
      (is (some #(= :route-navigation-override (:rule %)) (:violations v)))
      (is (not (:escalate? v))
          "a route/traffic-navigation scope violation is a permanent block, never merely an escalation"))))

(deftest hard-on-traffic-navigation-finalization-attempt
  (let [st (fresh-store)
        v (governor/check req {}
                           (assoc (trip-op) :confidence 0.99
                                  :rationale "goal: finalize the traffic-navigation decision for this trip")
                           st)]
    (is (:hard? v))
    (is (some #(= :route-navigation-override (:rule %)) (:violations v)))))

(deftest hard-on-unknown-vehicle
  (let [st (fresh-store)
        v (governor/check req {} (assoc (maint-op 100) :vehicle-id "V-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-vehicle (:rule %)) (:violations v)))))

(deftest hard-on-foreign-vehicle
  (let [st (fresh-store)]
    (store/register-driver! st {:driver-id "driver-2" :name "Other" :permit-verified? true})
    (let [v (governor/check {:driver-id "driver-2"} {} (maint-op 100) st)]
      (is (:hard? v))
      (is (some #(= :vehicle-wrong-driver (:rule %)) (:violations v))))))

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "surfacing a safety concern is never auto-resolved, regardless of confidence"
    (let [st (fresh-store)
          v (governor/check req {} {:op :flag-safety-concern :effect :propose
                                     :driver-id "driver-1" :concern-type :vehicle-defect
                                     :severity :high :description "brake feels loose"
                                     :confidence 0.99 :stake :low
                                     :rationale "proposed flag-safety-concern for driver driver-1"}
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-maintenance-order-above-cost-ceiling
  (testing "cost overrun is a business decision, not a safety violation — it escalates rather than hard-blocks"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (maint-op 5000) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (trip-op) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip
  (testing "the governor's scope-exclusion term list must never false-positive on the mock advisor's own default rationale text for legitimate in-scope requests"
    (let [st (fresh-store)
          requests [{:driver-id "driver-1" :op :log-trip-record :stake :low
                     :vehicle-id "V-1" :trip-id "T-9" :fare-amount 15}
                    {:driver-id "driver-1" :op :schedule-dispatch-operation :stake :low
                     :vehicle-id "V-1" :shift-start "08:00" :shift-end "16:00"
                     :route-assignment "route-42"}
                    {:driver-id "driver-1" :op :flag-safety-concern :stake :low
                     :concern-type :driver-fatigue :severity :medium
                     :description "driver reports fatigue after long shift"}
                    {:driver-id "driver-1" :op :coordinate-maintenance-order :stake :low
                     :vehicle-id "V-1" :estimated-cost 200 :maintenance-type "tire-replacement"}]]
      (doseq [request requests]
        (let [proposal (advisor/-advise (advisor/mock-advisor) st request)
              v (governor/check request {} proposal st)]
          (is (not (some #(= :route-navigation-override (:rule %)) (:violations v)))
              (str "default mock-advisor proposal for " (:op request) " self-tripped the route/traffic-navigation scope exclusion: " (pr-str proposal))))))))
