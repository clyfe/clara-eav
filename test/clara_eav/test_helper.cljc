(ns clara-eav.test-helper
  (:require
    #?@(:clj [[clojure.spec.test.alpha :as st]]
        :cljs [[cljs.spec.test.alpha :as st]])))

(defn spec-fixture [f]
  (st/instrument)
  (f)
  (st/unstrument))
