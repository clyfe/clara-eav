(ns clara-eav.rules-test
  (:require [clara-eav.test-helper :as test-helper]
    #?@(:clj [[clara.rules :as rules]
            [clara-eav.rules :as eav.rules]
            [clojure.test :refer [deftest testing is are use-fixtures]]]
        :cljs [[clara.rules :as rules :include-macros true]
               [clara-eav.rules :as eav.rules :include-macros true]
               [cljs.test :refer-macros [deftest testing is are
                                         use-fixtures]]])))

(use-fixtures :once test-helper/spec-fixture)

;; Entity maps

(def new1 #:todo{:eav/eid :new :text "..." :done false})
(def milk1 #:todo{:text "Buy milk" :done false})
(def eggs1 #:todo{:text "Buy eggs" :done true})
(def flakes #:todo{:text "Buy flakes" :done false})

(def new2 (assoc new1 :todo/text "!!!"))
(def milk2 (assoc milk1 :eav/eid 1, :todo/text "Buy milk2"))
(def eggs2 (assoc eggs1 :eav/eid 2))
(def ham2 #:todo{:text "Buy ham" :done false})
(def cookie-a #:todo{:text "Buy cookie a" :done false})
(def cookie-b #:todo{:text "Buy cookie b" :done false})

(def toast5 #:todo{:eav/eid "toast-tempid" :text "Buy toast" :done true})
(def jam5 #:todo{:eav/eid -7 :text "Buy jam" :done true})

;; Rules

(eav.rules/defrule milk-and-flakes-r
  [[_ :todo/text "Buy milk"]]
  =>
  (eav.rules/upsert-unconditional! flakes))

(eav.rules/defrule milk2-and-cookies-r
  [[_ :todo/text "Buy milk2"]]
  =>
  (eav.rules/upsert! [cookie-a cookie-b]))

(eav.rules/defrule remove-r
  [[::remove :eav/transient ?e]]
  [?eav <- [?e ?a ?v]]
  =>
  (eav.rules/retract! ?eav))

;; Queries

(eav.rules/defquery todo-q [:?e]
  [?todo <- eav.rules/entity :from [[?e]]])

(eav.rules/defquery todos-q []
  [[?e :todo/text]]
  [?todos <- eav.rules/entities :from [[?e]]])

(eav.rules/defquery transients-q []
  [?transient <- :eav/transient])

;; Session

(eav.rules/defsession session
  'clara-eav.rules-test)

;; Helpers

(defn todo
  [session ?e]
  (-> (rules/query session todo-q :?e ?e)
      first
      :?todo))

(defn todos
  [session]
  (->> (rules/query session todos-q)
       (mapcat :?todos)))

(defn transients
  [session]
  (->> (rules/query session transients-q)
       (map :?transient)))

(defn upsert
  [session tx]
  (-> (eav.rules/upsert session tx)
      (rules/fire-rules)))

(defn retract
  [session tx]
  (-> (eav.rules/retract session tx)
      (rules/fire-rules)))

;; Tests

(deftest upsert-retract-accumulate-test
  (testing "Upsert, retract, accumulate entities"
    (let [
          ;; - upsert via call
          ;; - upsert-unconditional! via milk-and-flakes-r rule
          ;; - entity accumulator via todo-q query
          ;; - entities accumulator via todos-q query
          ;; - store maintenance
          session1 (upsert session [new1 milk1 eggs1])
          new1' (dissoc new1 :eav/eid)
          store1s {:max-eid 3
                   :eav-index {:new new1'
                               1 milk1
                               2 eggs1
                               3 flakes}}
          milk1s (assoc milk1 :eav/eid 1)
          eggs1s (assoc eggs1 :eav/eid 2)
          flakes1s (assoc flakes :eav/eid 3)
          all1 (list new1 milk1s eggs1s flakes1s)
          _ (are [x y] (= x y)
              new1 (todo session1 :new)
              all1 (todos session1)
              store1s (:store session1))

          ;; - upsert via call
          ;; - upsert! via milk2-and-cookies-r rule
          ;; - entity accumulator via todo-q query
          ;; - entities accumulator via todos-q query
          ;; - store maintenance
          session2 (upsert session1 [new2 milk2 ham2])
          new2' (dissoc new2 :eav/eid)
          milk2' (dissoc milk2 :eav/eid)
          store2s {:max-eid 6
                   :eav-index {:new new2'
                               1 milk2'
                               2 eggs1
                               3 flakes
                               4 ham2
                               5 cookie-a
                               6 cookie-b}}
          ham2s (assoc ham2 :eav/eid 4)
          cookie-as (assoc cookie-a :eav/eid 5)
          cookie-bs (assoc cookie-b :eav/eid 6)
          all2 (list eggs2 flakes1s new2 milk2 ham2s cookie-as cookie-bs)
          eggs3 (assoc eggs1 :eav/eid 2)
          _ (are [x y] (= x y)
              new2 (todo session2 :new)
              all2 (todos session2)
              store2s (:store session2))

          ;; - retract via call
          ;; - entity accumulator via todo-q query
          ;; - entities accumulator via todos-q query
          ;; - store maintenance
          session3 (retract session2 [new2 eggs3])
          store3s {:max-eid 6
                   :eav-index {1 milk2'
                               3 flakes
                               4 ham2
                               5 cookie-a
                               6 cookie-b}}
          all3 (list flakes1s milk2 ham2s cookie-as cookie-bs)
          _ (are [x y] (= x y)
              nil (todo session3 :new)
              all3 (todos session3)
              store3s (:store session3))

          ;; - upsert transient via call
          ;; - retract! via remove-r rule
          ;; - eav binding via transients-q query
          ;; - entities accumulator via todos-q query
          ;; - store maintenance
          session4 (upsert session3 [[::remove :eav/transient 5]
                                     [::remove :eav/transient 6]])
          store4s {:max-eid 6
                   :eav-index {1 milk2'
                               3 flakes
                               4 ham2}}
          all4 (list flakes1s milk2 ham2s)
          _ (are [x y] (= x y)
              all4 (todos session4)
              [] (transients session4)
              store4s (:store session4))

          ;; - upsert via call
          ;; - tempids (string and negative int) resolution
          ;; - eav binding via transients-q query
          ;; - entities accumulator via todos-q query
          ;; - store maintenance
          session5 (upsert session4 [toast5 jam5])
          toast5' (dissoc toast5 :eav/eid)
          jam5' (dissoc jam5 :eav/eid)
          store5s {:max-eid 8
                   :eav-index {1 milk2'
                               3 flakes
                               4 ham2
                               7 toast5'
                               8 jam5'}}
          toast5s (assoc toast5 :eav/eid 7)
          jam5s (assoc jam5 :eav/eid 8)
          all5 (list flakes1s milk2 ham2s jam5s toast5s)
          tempids5s {"toast-tempid" 7
                     -7 8}
          _ (are [x y] (= x y)
              (set all5) (set (todos session5))
              [] (transients session5)
              store5s (:store session5)
              tempids5s (:tempids session5))])))
