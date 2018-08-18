(ns ^:no-doc clara-eav.session
  "A session wrapper that keeps track of extra state in a state store."
  (:require [clara.rules.engine :as engine]
            [clara-eav.store :as store]
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(defrecord SessionWrapper [session store]

  engine/ISession

  (insert [_this facts]
    (SessionWrapper. (engine/insert session facts) store))

  (retract [_this facts]
    (SessionWrapper. (engine/retract session facts) store))

  (fire-rules [this]
    (engine/fire-rules this {}))

  (fire-rules [_this opts]
    (binding [store/*store* (atom store)]
      (let [session' (engine/fire-rules session opts)]
        (SessionWrapper. session' @store/*store*))))

  (query [_this query params]
    (engine/query session query params))

  (components [_this]
    (engine/components session)))

(defn session?
  "Returns true if `session` is an instance of `SessionWrapper`, false
  otherwise."
  [session]
  (instance? SessionWrapper session))

(s/def ::session session?)

(defn isession?
  "Returns true if `isession` implements `clara.rules.engine/ISession`, false
  otherwise."
  [isession]
  (satisfies? engine/ISession isession))

(s/def ::isession isession?)

(s/fdef wrap
  :args (s/cat :isession ::isession)
  :ret ::session)
(defn wrap
  "Wraps a Clara Rules Session with a `SessionWrapper` that keeps track of
  extra state in a store, as needed by ClaraEAV."
  [isession]
  (->SessionWrapper isession store/init))
