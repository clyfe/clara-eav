(ns clara-eav.test-rules
  #?(:clj (:require [clara-eav.rules :as eav.rules]
                    [clara.rules.compiler])
     :cljs (:require-macros
             [clara-eav.rules :as eav.rules] 
             [clara.rules.compiler])))

(def flakes #:todo{:text "Buy flakes" :done false})
(def cookie-a #:todo{:text "Buy cookie a" :done false})
(def cookie-b #:todo{:text "Buy cookie b" :done false})

(eav.rules/defquery todo-q [:?e]
  [?todo <- eav.rules/entity :from [[?e]]])

(eav.rules/defquery todos-q []
  [[?e :todo/text]]
  [?todos <- eav.rules/entities :from [[?e]]])

(eav.rules/defquery transients-q []
  [?transient <- [:eav/transient]])

(eav.rules/defrule milk-and-flakes-r
  [[_ :todo/text "Buy milk"]]
  =>
  (eav.rules/upsert-unconditional! flakes))

(eav.rules/defrule milk2-and-cookies-r
  [[_ :todo/text "Buy milk2"]]
  =>
  (eav.rules/upsert! [cookie-a cookie-b]))

(eav.rules/defrule remove-r
  [[:remove :eav/transient ?e]]
  [?eav <- [?e ?a ?v]]
  =>
  (eav.rules/retract! ?eav))
