(ns clara-eav.rules
  "Thin layer over Clara-Rules common API functions that simplifies working with
  EAV triplets, and adds some specific features like transient EAVs, upserts and
  accumulators that rebuild entity maps out of EAVs."
  (:require [clara.rules :as rules]
            [clara.rules.engine :as engine]
            [clara.rules.accumulators :as accumulators]
            [clara-eav.eav :as eav]
            [clara-eav.session :as session]
            [clara-eav.store :as store]
    #?@(:clj [[clojure.spec.alpha :as s]
              [clara-eav.dsl :as dsl]]
        :cljs [[cljs.spec.alpha :as s]]))
  #?(:cljs (:require-macros
             [clara.rules :as rules])))

;; Transient EAVs maintenance

(rules/defrule ^:no-doc retract-transients
  {:salience -1000000000}
  [?d <- :eav/transient]
  =>
  (rules/retract! ?d))

;; Session

(defmacro defsession
  "Wrapper on  Clara-Rules defsession with EAV's `:fact-type-fn` and
  `:ancestors-fn`. Pass name and namespaces `(defsession my-session 'my.rules
  'my.more.rules)`."
  [name & nss]
  `(do (rules/defsession ~name 'clara-eav.rules ~@nss
         :fact-type-fn eav/fact-type-fn
         :ancestors-fn eav/ancestors-fn)
       ~(if (:ns &env)
          `(set! ~name (session/wrap ~name))
          `(alter-var-root #'~name session/wrap))))

;; Rules and Queries

(defmacro defrule
  "Like Clara-Rules defrule but with support for EAV fact expressions."
  [name & form]
  `(dsl/defrule ~name ~@form))

(defmacro defquery
  "Like Clara-Rules defquery but with support for EAV fact expressions."
  [name & form]
  `(dsl/defquery ~name ~@form))

;; Retractions

(s/fdef retract
  :args (s/cat :session ::session/session
               :tx ::eav/tx)
  :ret ::session/session)
(defn retract
  "Like Clara-Rules retract; tx is transaction data with no tempids."
  [session tx]
  (let [{:keys [retractables]
         :as store} (store/-eavs (:store session) (eav/eav-seq tx))]
    (assoc (engine/retract session retractables)
      :store (store/state store))))

(s/fdef retract!
  :args (s/cat :tx ::eav/tx))
(defn retract!
  "Like Clara-Rules retract!; tx is transaction data with no tempids."
  [tx]
  (let [{:keys [retractables]
         :as store} (store/-eavs @store/*store* (eav/eav-seq tx))]
    (when (seq retractables)
      (reset! store/*store* (store/state store))
      (engine/rhs-retract-facts! retractables))))

;; Upserts

(s/fdef upsert
  :args (s/cat :session ::session/session
               :tx ::eav/tx)
  :ret ::session/session)
(defn upsert
  "Similar to Clara-Rules insert-all; tx is transaction data. Retracts EAVs that
  have the same eid and attribute but with a new value. The returned session has
  an extra `:tempids` key with the resolved tempids map {tempid -> eid}."
  [session tx]
  (let [{:keys [insertables retractables tempids]
         :as store} (store/+eavs (:store session) (eav/eav-seq tx))]
    (cond-> session
            (seq retractables) (engine/retract retractables)
            (seq insertables) (engine/insert insertables)
            true (assoc :store (store/state store)
                        :tempids tempids))))

(s/fdef upsert!*
  :args (s/cat :tx ::eav/tx
               :unconditional boolean?))
(defn- upsert!*
  [tx unconditional]
  (let [{:keys [insertables retractables]
         :as store} (store/+eavs @store/*store* (eav/eav-seq tx))]
    (when (seq retractables)
      (engine/rhs-retract-facts! retractables))
    (when (seq insertables)
      (reset! store/*store* (store/state store))
      (engine/insert-facts! insertables unconditional))))

(s/fdef upsert!
  :args (s/cat :tx ::eav/tx))
(defn upsert!
  "Similar to Clara-Rules insert-all!; tx is transaction data. Retracts EAVs
  that have the same eid and attribute but come with a new value."
  [tx]
  (upsert!* tx false))

(s/fdef upsert-unconditional!
  :args (s/cat :tx ::eav/tx))
(defn upsert-unconditional!
  "Similar to Clara-Rules insert-all-unconditional!; tx is transaction data.
  Retracts EAVs that have the same eid and attribute but with a new value."
  [tx]
  (upsert!* tx true))

;; Accumulators

(defn- assert-eids=
  "Throws ExceptionInfo if `e` and `e'` are not equal."
  [e e']
  (when (not= e e')
    (throw (ex-info (str "An entity's EAVs must have same eid, "
                         "but found " e " and " e')
                    {:e e, :e' e'}))))

(defn- group->entity [[e eavs]]
  (reduce (fn [entity [e' a v]]
            (assert-eids= e e')
            (assoc entity a v))
          {:db/id e} eavs))

(defn- groups->entities
  "Given a map of EAVs groups, grouped by eid, builds and returns the entities
  corresponding to each group."
  [groups]
  (map group->entity groups))

(defn- groups->first-entity
  "Given a map of EAVs groups, grouped by eid, builds and returns the entity
  corresponding to the first group. If there is more than one group in the map
  throws an ExceptionInfo."
  [groups]
  (when (< 1 (count groups))
    (throw (ex-info "Query returned multiple entities" {:groups groups})))
  (group->entity (first groups)))

(def entities
  "Clara-Rules Accumulator that accumulates EAVs in a list of entity maps.
  Ex: `[?all-entities <- ce/entities :from [[:eav/all]]`."
  (accumulators/grouping-by :e groups->entities))

(def entity
  "Clara-Rules Accumulator that accumulates EAVs in an entity map.
  Make sure to filter such that all EAVs returned have the eid, or you get 
  whatever entity is first. Ex: `[?new-todo-template <- ce/entity :from 
  [[:new-todo]]]`."
  (accumulators/grouping-by :e groups->first-entity))
