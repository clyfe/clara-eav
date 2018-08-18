(ns clara-eav.rules-test
  (:require [clara-eav.test-rules :as test-rules]
    #?@(:clj [[clara.rules :as rules]
            [clara-eav.rules :as eav.rules]
            [clojure.test :refer [deftest testing is are]]]
        :cljs [[clara.rules :as rules :include-macros true]
               [clara-eav.rules :as eav.rules :include-macros true]
               [cljs.test :refer-macros [deftest testing is are run-tests]]])))

(def new1 #:todo{:db/id :new :text "..." :done false})
(def milk1 #:todo{:text "Buy milk" :done false})
(def eggs1 #:todo{:text "Buy eggs" :done true})

(def new2 (assoc new1 :todo/text "!!!"))
(def milk2 (assoc milk1 :db/id 1, :todo/text "Buy milk2"))
(def eggs2 (assoc eggs1 :db/id 2))
(def ham2 #:todo{:text "Buy ham" :done false})

(def toast5 #:todo{:db/id "toast-tempid" :text "Buy toast" :done true})
(def jam5 #:todo{:db/id -7 :text "Buy jam" :done true})

(eav.rules/defsession session
  'clara-eav.test-rules)

(defn todo
  [session ?e]
  (-> (rules/query session test-rules/todo-q :?e ?e)
      first
      :?todo))

(defn todos
  [session]
  (->> (rules/query session test-rules/todos-q)
       (mapcat :?todos)))

(defn transients
  [session]
  (->> (rules/query session test-rules/transients-q)
       (map :?transient)))

(defn upsert
  [session tx]
  (-> (eav.rules/upsert session tx)
      (rules/fire-rules)))

(defn retract
  [session tx]
  (-> (eav.rules/retract session tx)
      (rules/fire-rules)))

(deftest upsert-accumulate-test
  (testing "Upsert, retract, accumulate entities"
    (let [session1 (upsert session [new1 milk1 eggs1])
          store1s {:max-eid 3
                   :eav-index {:new (dissoc new1 :db/id)
                               1 milk1
                               2 eggs1
                               3 test-rules/flakes}}
          milk1s (assoc milk1 :db/id 1)
          eggs1s (assoc eggs1 :db/id 2)
          flakes1s (assoc test-rules/flakes :db/id 3)
          all1 (list new1 milk1s eggs1s flakes1s)
          _ (are [x y] (= x y)
              new1 (todo session1 :new)
              all1 (todos session1)
              store1s (:store session1))

          session2 (upsert session1 [new2 milk2 ham2])
          store2s {:max-eid 6
                   :eav-index {:new (dissoc new2 :db/id)
                               1 (dissoc milk2 :db/id)
                               2 eggs1
                               3 test-rules/flakes
                               4 ham2
                               5 test-rules/cookie-a
                               6 test-rules/cookie-b}}
          ham2s (assoc ham2 :db/id 4)
          cookie-as (assoc test-rules/cookie-a :db/id 5)
          cookie-bs (assoc test-rules/cookie-b :db/id 6)
          all2 (list eggs2 flakes1s new2 milk2 ham2s cookie-as cookie-bs)
          eggs3 (assoc eggs1 :db/id 2)
          _ (are [x y] (= x y)
              new2 (todo session2 :new)
              all2 (todos session2)
              store2s (:store session2))

          session3 (retract session2 [new2 eggs3])
          store3s {:max-eid 6
                   :eav-index {1 (dissoc milk2 :db/id)
                               3 test-rules/flakes
                               4 ham2
                               5 test-rules/cookie-a
                               6 test-rules/cookie-b}}
          all3 (list flakes1s milk2 ham2s cookie-as cookie-bs)
          _ (are [x y] (= x y)
              nil (todo session3 :new)
              all3 (todos session3)
              store3s (:store session3))

          session4 (upsert session3 [[:remove :eav/transient 5]
                                     [:remove :eav/transient 6]])
          store4s {:max-eid 6
                   :eav-index {1 (dissoc milk2 :db/id)
                               3 test-rules/flakes
                               4 ham2}}
          all4 (list flakes1s milk2 ham2s)
          _ (are [x y] (= x y)
              all4 (todos session4)
              [] (transients session4)
              store4s (:store session4))

          session5 (upsert session4 [toast5 jam5])
          store5s {:max-eid 8
                   :eav-index {1 (dissoc milk2 :db/id)
                               3 test-rules/flakes
                               4 ham2
                               7 (dissoc toast5 :db/id)
                               8 (dissoc jam5 :db/id)}}
          toast5s (assoc toast5 :db/id 7)
          jam5s (assoc jam5 :db/id 8)
          all5 (list flakes1s milk2 ham2s jam5s toast5s)
          _ (are [x y] (= x y)
              (set all5) (set (todos session5))
              [] (transients session5)
              store5s (:store session5))])))
