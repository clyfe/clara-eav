(ns clara-eav.eav-test
  (:require [clara-eav.test-helper :as test-helper]
    #?@(:clj [[clojure.test :refer [deftest testing is are use-fixtures]]
              [clojure.spec.alpha :as s]
              [clara-eav.eav :as eav]]
        :cljs [[cljs.test :refer-macros [deftest testing is are use-fixtures]]
               [cljs.spec.alpha :as s]
               [clara-eav.eav :as eav :refer [EAV]]])
              [clojure.set :as set])
  #?(:clj (:import 
            (clara_eav.eav EAV)
            (clojure.lang Associative)
            (java.util UUID))))

(use-fixtures :once test-helper/spec-fixture)

(deftest eav-test
  (testing "Constructing and destructuring a eav."
    (let [[e a v] (eav/->EAV 1 :todo/done false)]
      (is (= [1 :todo/done false] [e a v])))))

(defrecord MyRecord [x])

(deftest fact-type-fn-test
  (testing "Fact type of eav."
    (let [d (eav/->EAV 1 :todo/done false)]
      (is (= :todo/done (#'eav/fact-type-fn d)))))
  (testing "Fact type of non-eav."
    (let [r (->MyRecord 1)]
      (is (= MyRecord (#'eav/fact-type-fn r))))))

(deftest ancestors-fn-test
  (testing "Ancestors of keyword."
    (is (= [EAV :eav/all] (#'eav/ancestors-fn :eav/eid))))
  #?(:clj (testing "Ancestors of my record."
            (is (some #{Associative}
                      (#'eav/ancestors-fn MyRecord))))))

(def eav-record (eav/->EAV 1 :todo/done true))
(def eav-vector [1 :todo/done true])

(defn rand-uuid []
  #?(:clj (UUID/randomUUID)
     :cljs (random-uuid)))

(deftest eav-spec-test
  (testing "Are eav records."
    (are [x y] (= x (s/valid? ::eav/eav y))
      true eav-record
      true eav-vector
      true (assoc eav-vector 0 (rand-uuid))
      false (assoc eav-record :a "not-a-keyword")
      false (assoc eav-vector 1 "not-a-keyword")
      false (conj eav-vector "4th element")
      false 1)))

(def new-todo
  {:todo/text "Buy milk"
   :todo/done false})

(def saved-todo
  {:eav/eid 10
   :todo/text "Buy eggs"
   :todo/done true})

(def saved-todo-eavs
  #{(eav/->EAV 10 :todo/text "Buy eggs")
    (eav/->EAV 10 :todo/done true)})

(deftest entity->eav-seq-test
  (testing "Entity map to EAVs with tempids"
    (let [eav-seq (#'eav/entity->eav-seq new-todo)
          eids (map (comp second first) eav-seq)]
      (is (= 2 (count eav-seq)))
      (is (every? string? eids))
      (is (apply = eids))))
  (testing "Entity map to EAVs with eids"
    (is (= saved-todo-eavs (set (#'eav/entity->eav-seq saved-todo))))))

(def list1 (list eav-record))
(def list2 (list eav-record eav-record))

(defn eid-of [eavs a' v']
  (let [=av (fn [[e a v]]
              (when (and (= a a') (= v v'))
                e))
        eid (some =av eavs)]
    (if eid
      eid
      (throw (ex-info (str "EID for " a' " and " v' " not found") {})))))

(deftest eav-seq-test
  (testing "Converts EAVs and lists of EAVs to a eav sequence"
    (are [x y] (= x (eav/eav-seq y))
      list1 eav-record
      list1 eav-vector
      list1 list1
      list1 (list eav-vector)
      list2 list2
      list2 (list eav-vector eav-vector)
      list2 (list eav-vector eav-record)
      list2 (list eav-record eav-vector)))
  (testing "Converts a new entity map to a eav sequence"
    (let [eav-seq (eav/eav-seq new-todo)
          eid1 (eid-of eav-seq :todo/text "Buy milk")
          eid2 (eid-of eav-seq :todo/done false)]
      (is (= 2 (count eav-seq)))
      (is (= eid1 eid2))
      (is (string? eid1))
      (is (string? eid2))))
  (testing "Converts a saved entity map to a eav sequence"
    (let [eav-seq (eav/eav-seq saved-todo)]
      (is (= saved-todo-eavs
             (set eav-seq)))))
  (testing "Converts a list of entity maps to a EAVs sequence"
    (let [eav-seq (eav/eav-seq (list new-todo saved-todo))
          eid1 (eid-of eav-seq :todo/text "Buy milk")
          eid2 (eid-of eav-seq :todo/done false)]
      (is (= 4 (count eav-seq)))
      (is (= eid1 eid2))
      (is (string? eid1))
      (is (string? eid2))
      (is (set/subset? saved-todo-eavs (set eav-seq))))))
