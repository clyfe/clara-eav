## Guide

This guide assumes familiarity with [Clara Rules](https://www.clara-rules.org). 
The `clara-eav.rules` namespace mirrors `clara.rules` for the most part. 

```clojure
(ns my.sample
  (:require [clara.rules :as r]
            [clara-eav.rules :as er]))
```

### Concepts

#### EAV triplets

In Clara you store n-tuples (arbitrary records). In ClaraEAV you store only one 
type of record, the Entity-Attribute-Value (EAV) triplet. Standard records as 
normally operated by Clara can still be used alongside in the same session. The 
equivalence between n-tuples and triplets is illustrated below:

```clojure
;; Sample N-tuple (4-tuple): a todo with 4 positions 
#:todo{:db/id 1, :text "Buy milk", :done false, :tag :buy}

;; EAVs list (triplets list) equivalent to the n-tuple above
[[1 :db/id 1]
 [1 :todo/text "Buy milk"]
 [1 :todo/done false]
 [1 :todo/tag :buy]]
```

EAVs are of type record `clara_eav.eav.EAV` and can be destructured as 3-valued 
vectors or as maps with `:e :a :v` keys.

#### Transaction data (tx)

Upserts and retractions are done using "transaction data" (tx). It can be:

1. A vector of 3 elements `[e a v]`, "a" being a keyword.
2. An `EAV` instance `(clara-eav.eav/->EAV e a v)`.
3. A sequence of EAVs like `[d1 d2 ...]`, with d1, d2 etc as defined above.
4. An entity map `{:db/id 1 :todo/text "Buy milk", :todo/done false}`.
5. A sequence of entity maps.

Transaction data (tx) is what functions usually work with (upsert/retract). 
Since we use the generic "transaction data" concept, there is no -all- variants 
for upserts like in Clara, and standard upsert can be used for all cases.

The "e" position we sometimes name :db/id, **eid** or entity id.

EAVs with the same eid form entity maps, their attributes (a) and values (v)
being keys and values in the entity map, and :db/id being a key in the entity
map pointing to the eid (e); :db/id presence results in an operation with
upsert semantics, and absence in results insert semantics.

#### Tempids

A string eid designates a tempid and so does a negative int. A tempid is a used
in upserts as placeholder to be be replaced with with unique eids generated from
a per-session integer sequence. In the same transaction data, EAVs with the same
tempid will be saved with the same eid. Tempids are not allowed in retractions.

```clojure
;; The transaction data
[["x" :a 1]["x" :b 2]["y" :a 3]["y" :b 4][-8 :a 5][-8 :b 6]]
;; on upsert becomes
[[1 :a 1][1 :b 2][2 :a 3][2 :b 4][3 :a 5][3 :b 6]]
```

#### Transient EAVs 

EAVs with `:eav/transient` attribute, when upserted, run through the rule chain 
and activate rules that refer them, but are retracted at the end (at salience -1 
billion), and don't end up being saved. They can be used to implement commands.

```clojure
[:my-command :eav/transient "my-value"]
```

#### Fact DSL

Rules and Queries can use the `[E A V]` syntax sugar in fact definition. The 
following fact pairs are equivalent (notice the extra `[]` vector brackets):

```clojure
[[?e :todo/text ?v]]     => [:todo/text (= (:e this) ?e) 
                                        (= (:v this) ?v)]
[[?e :todo/done]]        => [:todo/done (= (:e this) ?e)]

[[_]]                    => [:eav/all]
[[?e]]                   => [:eav/all (= (:e this) ?e)]
[[?e ?a]]                => [:eav/all (= (:e this) ?e) 
                                      (= (:a this) ?a)]
[[?e ?a ?v]]             => [:eav/all (= (:e this) ?e) 
                                      (= (:a this) ?a) 
                                      (= (:v this) ?v)]

[?eavs <- [?e ?a ?v]]    => [?eavs <- :eav/all (= (:e this) ?e) 
                                               (= (:a this) ?a) 
                                               (= (:v this) ?v)]

[?x <- accum :from [[_]] => [?x <- accumulator :from [:eav/all]]
```

Keywords can be also be used as types, and they match the EAVs attribute. The 
attribute `:a` of an EAV is used as it's Clara type. The `:eav/all` keyword used
as a type matches all EAVs.

```clojure
;; Clara fact syntax
[?eav <- :todo/text (= (:e this) :new)]
[?eav <- (a/all) :from [:eav/all (= (:e this) :new)]]

;; ClaraEAV fact syntax
[?eav <- [:new :todo/text]]
[?eav <- (a/all) :from [[:new]]]
```

### API

#### Sessions

Sessions created trough ClaraEAV api are configured to work with EAVs.

```clojure
(er/defsession session 'my.rules 'my.other.rules)
```

#### Rules and Queries

Define rules and queries using the two provided macros, `er/defrule` and 
`er/defquery`, in which you can use the ClaraEAV fact DSL:

```clojure
(er/defrule logged-in
  {:salience 1000}
  [[:session :email]]
  =>
  (upsert! [:global :layout :todos]))

(er/defquery done-todos []
  [[?e :todo/done true]]
  [?todos <- er/entities :from [[?e]])
```

#### Transact EAV triplets

In ClaraEAV there is no plain insert operation, only upsert and retract. 
Functions are somewhat similar with those in Clara.

##### Upsert 

```clojure
;; Upsert an EAV; if the attribute :layout exists under the eid :global, it's 
;; value will be updated to :login, otherwise the triplet will be inserted.
;; Like in Clara use functions not ending in exclamation mark are used 
;; outside of rules and take the session parameter.
(er/upsert session [:global :layout :login])

;; Upsert a list of EAVs. Like in Clara use functions ending in exclamation mark
;; are used inside rules, with truth maintenance.
(er/upsert! [[:new :todo/text "..."] 
             [:new :todo/done false]])

;; Insert an entity map, from witin a rule. The eid is allocated automatically. 
;; Like in Clara, the unconditional variant does not do truth maintenance.
(er/upsert-unconditional! {:todo/text "Buy milk"
                           :todo/done false})

;; The entity EAVs are inserted, or updated if they already exist.
(er/upsert! {:db/id :new
             :todo/text "Buy milk"
             :todo/done false})

;; The two EAVs will form one entity, "todo-id" tempid (string) being replaced
;; with the same generated eid for the given transaction data.
(er/upsert! [["todo-id" :todo/text "..."]
             ["todo-id" :todo/done false]])

;; Same as previous in map form.
(er/upsert! {:db/eid "todo-id"
             :todo/text "..."
             :todo/done false})

```

##### Retract

```clojure
;; Retract a list of entity maps in a call outside of rules.
(er/retract session [{:todo/text "Buy milk"
                      :todo/done false}
                     {:todo/text "Buy eggs"
                      :todo/done true}])

;; Retract an EAV from within a rule, with truth maintenance.
(er/retract! [5 :todo/done true])
```

#### Accumulators

Accumulators are provided to turn EAVs back into entity maps upon retrieval.
 
1. `er/entity` builds one entity map from a list of EAVs (they must all share 
the same eid, otherwise an exception is thrown) and binds it to the given 
variable.
2. `er/entities` builds a list of entity maps from a list of EAVs, and binds 
them to the given variable.

```clojure
(er/defquery todo [:?e]
  [?todo <- er/entity :from [[?e]]])

(er/defquery done-todos []
  [[?e :todo/done true]]
  [?todos <- er/entities :from [[?e]])
```
