(ns clara-eav.session-test
  (:require [clara-eav.test-helper :as test-helper]
            [clara.rules.engine :as engine]
            [clara-eav.store :as store]
            [clara-eav.session :as session]
            #?(:clj [clojure.test :refer [deftest testing is are use-fixtures]]
               :cljs [cljs.test :refer-macros [deftest testing is are use-fixtures]])))

(use-fixtures :once test-helper/spec-fixture)

(def ^:private inc0 (fnil inc 0))

(defn spy [calls f]
  (reify engine/ISession
    (insert [_session _facts]
      (swap! calls update :insert inc0))
    (retract [_session _facts]
      (swap! calls update :retract inc0))
    (fire-rules [_session]
      (f)
      (swap! calls update :fire-rules inc0))
    (fire-rules [_session _opts]
      (f)
      (swap! calls update :fire-rules inc0))
    (query [_session _query _params]
      (swap! calls update :query inc0))
    (components [_session]
      (swap! calls update :components inc0))))

(deftest session-test

  (testing "Session delegation"
    (let [calls (atom {})
          session (spy calls (constantly true))
          wrapper (session/wrap session)]
      (are [x] (session/session? x)
        (engine/insert wrapper [])
        (engine/retract wrapper [])
        (engine/fire-rules wrapper)
        (engine/fire-rules wrapper {}))
      (engine/query wrapper 'some-query {})
      (are [f c] (= (f @calls) c)
        :insert 1
        :retract 1
        :query 1
        :fire-rules 2)))

  (testing "Store binding"
    (let [binded? (fn [& _] (is (= store/init @store/*store*)))
          calls (atom {})
          session (spy calls binded?)
          wrapper (session/wrap session)]
      (engine/fire-rules wrapper)
      (is (= 1 (:fire-rules @calls))))))
