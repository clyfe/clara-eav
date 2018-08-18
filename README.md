## About

ClaraEAV is a thin layer over Clara-Rules API that simplifies working with
[EAV triplets](https://en.wikipedia.org/wiki/Entity%E2%80%93attribute%E2%80%93value_model),
similar to [Triplestores](https://en.wikipedia.org/wiki/Triplestore).

The main benefit of EAV triplets over Clara's default n-tuples (arbitrary
records) is that updates (equivalent to a retraction and an insertion) are local
to the attribute (EAV triplet). Updating an n-tuple requires retraction and re-
insertion of the whole tuple. The downside is extra joins are needed to build 
back an entity from it's constituent EAVs.

## Installation

Clara releases are on [Clojars](https://clojars.org/). Add the following to your
project dependencies:

```clojure
[clyfe/clara-eav "0.1.0"]
```

## Usage

You should be familiar with [Clara Rules](https://www.clara-rules.org) before 
using ClaraEAV. 

The `clara-eav.rules` namespace mirrors `clara.rules` for the most 
part, and adds two accumulators to build back entity maps from stored EAV 
triplets. 

For more information see the [Guide](doc/guide.md) and 
[API Docs](https://cljdoc.xyz/d/clyfe/clara-eav/CURRENT). Tests can be useful, 
see: [test_rules.cljc](test/clara_eav/test_rules.cljc) for rules and queries 
definition and [rules_test.cljc](test/clara_eav/rules_test.cljc) for sample 
usage.

```clojure
(ns sample
  (:require [clara.rules :as r]
            [clara-eav.rules :as er]))

(er/defquery todo-q [:?e] 
  [?todo <- er/entity :from [[?e]]])

(er/defsession session 
  'sample)

(def tx
  #:todo{:db/id :new
         :text "..."
         :done false})

(-> session
    (er/upsert tx)
    (r/fire-rules)
    (r/query todo-q :?e :new))

; ({:?e :new, :?todo #:todo{:db/id :new, :text "...", :done false}})
```

## Credits

* [Clara Rules](http://www.clara-rules.org/) the library this is built on
* [Precept](https://github.com/CoNarrative/precept) the origin of all ideas
* [FactUI](https://github.com/arachne-framework/factui) DSL parsing ideas
