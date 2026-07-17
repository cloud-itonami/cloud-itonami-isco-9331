(ns handpedaldispatch.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [handpedaldispatch.actor :as actor]
            [handpedaldispatch.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-driver! st {:driver-id "driver-1" :name "Kobo Rickshaw"
                                 :permit-verified? true})
    (store/register-vehicle! st {:vehicle-id "V-1" :driver-id "driver-1"
                                  :name "cart-042"
                                  :maintenance-cost-ceiling 500})
    st))

(deftest commits-a-trip-record-log
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :log-trip-record :stake :low
                  :vehicle-id "V-1" :trip-id "T-1" :fare-amount 12}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "driver-1"))))))

(deftest commits-a-dispatch-schedule
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :schedule-dispatch-operation :stake :low
                  :vehicle-id "V-1" :shift-start "08:00" :shift-end "16:00"
                  :route-assignment "route-42"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "driver-1"))))))

(deftest holds-an-out-of-allowlist-op
  (testing "closed op-allowlist enforced end-to-end: a request whose op is not one of the 4 registered ops is held, never committed"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:driver-id "driver-1" :op :finalize-route-decision :stake :low
                    :vehicle-id "V-1"}
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "driver-1"))))))

(deftest holds-for-unregistered-driver
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "nobody" :op :log-trip-record :stake :low
                  :vehicle-id "V-1" :trip-id "T-1" :fare-amount 12}
        result (actor/run-request! graph request {} "thread-4")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "nobody")))))

(deftest interrupts-then-approves-safety-concern-on-human-approval
  (testing "a safety-concern flag always escalates for human sign-off, and only commits after explicit approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:driver-id "driver-1" :op :flag-safety-concern :stake :low
                    :vehicle-id "V-1" :concern-type :vehicle-defect :severity :high
                    :description "brake feels loose"}
          interrupted (actor/run-request! graph request {} "thread-5")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "driver-1")))
      (let [resumed (actor/approve! graph "thread-5")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of st "driver-1"))))))))

(deftest interrupts-then-approves-over-ceiling-maintenance-order
  (testing "a maintenance order above the vehicle's registered cost ceiling escalates rather than hard-blocking, and commits only after human approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:driver-id "driver-1" :op :coordinate-maintenance-order :stake :low
                    :vehicle-id "V-1" :estimated-cost 5000 :maintenance-type "engine-overhaul"}
          interrupted (actor/run-request! graph request {} "thread-6")]
      (is (= :interrupted (:status interrupted)))
      (is (empty? (store/records-of st "driver-1")))
      (let [resumed (actor/approve! graph "thread-6")]
        (is (= :done (:status resumed)))
        (is (= 1 (count (store/records-of st "driver-1"))))))))
