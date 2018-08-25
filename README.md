[![Clojars](https://img.shields.io/clojars/v/clyfe/clara-eav.svg)](https://clojars.org/clyfe/clara-eav)
[![Cljdoc](https://cljdoc.xyz/badge/clyfe/clara-eav)](https://cljdoc.xyz/d/clyfe/clara-eav/CURRENT)
[![CircleCI](https://circleci.com/gh/clyfe/clara-eav.svg?style=shield)](https://circleci.com/gh/clyfe/clara-eav)
[![Codecov](https://codecov.io/gh/clyfe/clara-eav/branch/master/graph/badge.svg)](https://codecov.io/gh/clyfe/clara-eav)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/clyfe/clara-eav/blob/master/LICENSE)

#### About

ClaraEAV is a thin layer over Clara-Rules API that simplifies working with
[EAV triplets](https://en.wikipedia.org/wiki/Entity%E2%80%93attribute%E2%80%93value_model),
similar to [Triplestores](https://en.wikipedia.org/wiki/Triplestore). It works
with both Clojure and ClojureScript.

The main benefit of EAV triplets over Clara's default n-tuples (arbitrary
records) is that updates (equivalent to a retraction and an insertion) are local
to the attribute (EAV triplet). Updating an n-tuple requires retraction and 
re-insertion of the whole tuple. The downside is extra joins are needed to build 
back an entity from it's constituent EAVs.

#### Installation

ClaraEAV releases are on [Clojars](https://clojars.org/clyfe/clara-eav).

#### Usage

You should be familiar with [Clara Rules](https://www.clara-rules.org) before 
using ClaraEAV. See the documentation, including the 
[Guide](https://cljdoc.xyz/d/clyfe/clara-eav/CURRENT/doc/guide), on 
[Cljdoc](https://cljdoc.xyz/d/clyfe/clara-eav/CURRENT). For a more complete
example see the [rules test](test/clara_eav/rules_test.cljc).

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

#### Credits

* [Clara Rules](http://www.clara-rules.org/) the library this is built on
* [Precept](https://github.com/CoNarrative/precept) the origin of all ideas
* [FactUI](https://github.com/arachne-framework/factui) DSL parsing ideas
