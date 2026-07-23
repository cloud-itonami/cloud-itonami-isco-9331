(ns handpedaldispatch.advisor
  "Hand/Pedal Vehicle Dispatch Advisor — the advisor named in this
  repository's README, proposing a dispatch/logistics coordination
  operation (log a trip/fare/incident record, schedule a driver-roster
  dispatch assignment, flag a safety concern, coordinate a maintenance
  order) from a driver's registered profile and vehicle record.
  Swappable mock/llm; the advisor ONLY proposes —
  `handpedaldispatch.governor` independently checks driver-permit
  verification, the closed op-allowlist and the vehicle's registered
  maintenance-cost ceiling, and always escalates safety-concern flags.
  Modeled on cloud-itonami-isco-3313's advisor.

  This actor coordinates DISPATCH/LOGISTICS SCHEDULING ONLY. It never
  operates a rickshaw/pedicab/hand-cart, never finalizes a
  route/traffic-navigation decision and never overrides the driver's
  on-road safety judgment — that authority never leaves the human
  driver.

  A proposal: {:op :log-trip-record|:schedule-dispatch-operation|
               :flag-safety-concern|:coordinate-maintenance-order
               :effect :propose :driver-id str :stake kw
               :confidence n :rationale str, plus op-specific fields
               carried through from the request (:vehicle-id,
               :trip-id, :fare-amount, :shift-start, :shift-end,
               :route-assignment, :concern-type, :severity,
               :description, :estimated-cost, :maintenance-type).}"
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake driver-id] :as request}]
  (merge (dissoc request :stake)
         {:op op
          :effect :propose
          :driver-id driver-id
          :stake (or stake :low)
          :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
          :rationale (str "proposed " (name op) " for driver " driver-id)}))

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a hand/pedal-vehicle dispatch coordination advisor. Given a
   request, propose an :op (one of :log-trip-record,
   :schedule-dispatch-operation, :flag-safety-concern,
   :coordinate-maintenance-order), the :driver-id, any relevant
   op-specific fields, an honest :confidence and a :stake. You
   coordinate dispatch/logistics scheduling only — never propose an
   action that finalizes a route/traffic-navigation decision or
   overrides the driver's on-road safety judgment; that authority
   never leaves the human driver, and the governor treats any such
   proposal as a hard, permanent block. Safety concerns always require
   human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
