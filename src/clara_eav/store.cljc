(ns ^:no-doc clara-eav.store
  "A store keeps track of max-eid and maintains an EAV index."
  (:require [clara-eav.eav :as eav]
            [medley.core :as medley]
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(def ^:dynamic *store*
  "Dynamic atom of store to be used in rule productions, similar to other
  mechanisms from Clara."
  nil)

(s/def ::max-eid integer?)
(s/def ::eav-index map?)
(s/def ::insertables ::eav/record-seq)
(s/def ::retractables ::eav/record-seq)
(s/def ::tempids (s/map-of string? integer?))
(s/def ::store (s/keys :req-un [::max-eid ::eav-index]))
(s/def ::store-tx
  (s/keys :req-un [::max-eid ::eav-index]
          :opt-un [::insertables ::retractables ::tempids]))

(def init
  {:max-eid 0
   :eav-index {}})

(s/fdef state
  :args (s/cat :store ::store-tx)
  :ret ::store)
(defn state
  "Remove extra keys from intermediary steps of computations and returns just 
  the store state."
  [store]
  (select-keys store [:max-eid :eav-index]))

(s/def ::e
  (s/or :string string?
        :keyword keyword?
        :uuid uuid?
        :integer integer?))

(s/fdef tempid?
  :args (s/cat :e ::e)
  :ret boolean?)
(defn- tempid?
  "True if `e` is a tempid. Strings and negative ints are tempids; keywords,
  positive ints and uuids are not."
  [e]
  (or (string? e)
      (neg-int? e)))

(s/fdef -eav
  :args (s/cat :store ::store-tx
               :eav ::eav/record)
  :ret ::store-tx)
(defn- -eav
  "Substracts `eav` from `store` updating it's `:eav-index`. Returns the updated 
  `store` including `:retractables` eavs."
  [store eav]
  (let [{:keys [e a]} eav]
    (if (tempid? e)
      (throw (ex-info "Tempids not allowed in retractions" {:e e}))
      (-> store
          (update :retractables conj eav)
          (medley/dissoc-in [:eav-index e a])))))

(s/fdef -eavs
  :args (s/cat :store ::store
               :eavs ::eav/record-seq)
  :ret ::store-tx)
(defn -eavs
  "Called in retractions to obtain retractables. Throws if tempids are present
  in `eavs`, otherwise updates `store`'s `:eav-index`. Returns the updated store
  including `:retractables` eavs."
  [store eavs]
  (reduce -eav
          (assoc store :retractables [])
          eavs))

(s/fdef +eav
  :args (s/cat :store ::store-tx
               :eav ::eav/record)
  :ret ::store-tx)
(defn- +eav
  "Adds `eav` to `store` updating it's `:max-eid` and `:eav-index`. Returns the
  updated `store` including `:insertables` eavs, `:retractables` eavs and 
  resolved `:tempids` map of {tempid -> eid}."
  [store eav]
  (let [{:keys [tempids max-eid eav-index]} store
        {:keys [e a v]} eav]
    (if (tempid? e)
      (if-some [eid (get tempids e)]
        (-> store
            (update :insertables conj (assoc eav :e eid))
            (assoc-in [:eav-index eid a] v))
        (let [new-eid (inc max-eid)]
          (-> store
              (update :insertables conj (assoc eav :e new-eid))
              (assoc-in [:tempids e] new-eid)
              (assoc :max-eid new-eid)
              (assoc-in [:eav-index new-eid a] v))))
      (if (= :transient a)
        (update store :insertables conj eav)
        (if-some [v' (get-in eav-index [e a])]
          (cond-> store
                  (not= v v') (-> (update :insertables conj eav)
                                  (update :retractables conj (assoc eav :v v'))
                                  (assoc-in [:eav-index e a] v)))
          (-> store
              (update :insertables conj eav)
              (assoc-in [:eav-index e a] v)))))))

(s/fdef +eavs
  :args (s/cat :store ::store
               :eavs ::eav/record-seq)
  :ret ::store-tx)
(defn +eavs
  "Called in upserts to obtain insertables and retractables. Resolves tempids in
  `eavs` and updates `store`'s `:max-id` and `:eav-index`. Returns the updated
  store including `insertables` and `retractables` eavs and resolved tempids map
  {tempid -> eid}."
  [store eavs]
  (reduce +eav
          (assoc store :insertables []
                       :retractables []
                       :tempids {})
          eavs))
