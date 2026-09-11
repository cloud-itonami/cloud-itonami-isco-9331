(ns handpedaldispatch.store
  "SSoT for the ISCO-08 9331 hand and pedal vehicle dispatch
  coordination actor (itonami actor pattern, ADR-2607121000 /
  CLAUDE.md Actors section; README's 'Robotics premise' — a
  dispatch/logistics coordination robot performs driver-roster
  scheduling, trip/fare/incident-report logging, safety-concern
  surfacing and maintenance-order coordination for rickshaw/pedicab/
  hand-cart operations under this advisor/governor pair, which never
  operates the vehicle itself and never finalizes a route/traffic-
  navigation decision or overrides the driver's on-road safety
  judgment — that authority never leaves the human driver). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    driver  — a registered hand/pedal-vehicle driver with an
              independently verified operating permit
              (:driver-id :name :permit-verified?)
    vehicle — a registered vehicle/cart {:vehicle-id :driver-id :name
              :maintenance-cost-ceiling number}. `:maintenance-cost-ceiling`
              is the registered ceiling above which a
              :coordinate-maintenance-order proposal always escalates
              to human sign-off (never a hard block — cost overruns
              are a business decision, not a safety violation).
    record  — a committed operating record (a logged trip, a
              scheduled dispatch, a coordinated maintenance order) —
              written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold.")

(defprotocol Store
  (driver [s driver-id])
  (vehicle [s vehicle-id])
  (records-of [s driver-id])
  (ledger [s])
  (register-driver! [s d])
  (register-vehicle! [s v])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (driver [_ driver-id] (get-in @a [:drivers driver-id]))
  (vehicle [_ vehicle-id] (get-in @a [:vehicles vehicle-id]))
  (records-of [_ driver-id] (filter #(= driver-id (:driver-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-driver! [s d]
    (swap! a assoc-in [:drivers (:driver-id d)] d) s)
  (register-vehicle! [s v]
    (swap! a assoc-in [:vehicles (:vehicle-id v)] v) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:drivers {} :vehicles {} :records [] :ledger []}
                                    seed)))))
