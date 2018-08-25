(ns ^:no-doc clara-eav.dsl
  "Minor sugar over Clara-Rules Expressions such that fact expressions can also
  be given in `[E A V]` triplet form, expanding to `[A (= (:e this) E) (= (:v 
  this) V)]`. We define a parser over expressions that makes the conversions.
  Standard Clara-Rules Expressions remain valid syntax except the extreme case
  where you would want `fact-type-fn` to return vectors, which is unlikely."
  (:require [clojure.walk :as walk]
            [clojure.spec.alpha :as s]
            [clojure.core.match :as match]
            [clara.rules :as rules]))

(s/def ::defrule
  (s/cat ::docstring (s/? string?)
         ::properties (s/? map?)
         ::lhs ::lhs
         ::separator #{'=>}
         ::rhs (s/+ any?)))

(s/def ::defquery
  (s/cat ::docstring (s/? string?)
         ::params (s/coll-of keyword? :kind vector?)
         ::lhs ::lhs))

(s/def ::lhs (s/cat ::conditions (s/+ ::condition)))

(s/def ::condition
  (s/or ::fact ::fact
        ::boolean ::boolean
        ::test ::test
        ::accumulator ::accumulator))

(s/def ::fact (s/or ::fact-eav ::fact-eav
                    ::fact-clara ::fact-clara))

(s/def ::fact-clara
  (s/and vector?
         (s/conformer vec vec)
         (s/cat ::bind (s/? ::bind)
                ::bind-arrow (s/? #{'<-})
                ::type #(not (or (vector? %) (= '<- %)))
                ::destructure (s/? vector?)
                ::conditions (s/* ::sexp))))

(s/def ::fact-eav
  (s/and vector?
         (s/conformer vec vec)
         (s/cat ::bind (s/? ::bind)
                ::bind-arrow (s/? #{'<-})
                ::eav ::eav)))

(s/def ::eav
  (s/and vector?
         (s/conformer vec vec)
         #(<= 1 (count %) 3)
         (s/or ::eav1 (s/cat ::e ::e)
               ::eav2 (s/cat ::e ::e, ::a ::a)
               ::eav3 (s/cat ::e ::e, ::a ::a, ::v ::v))))

(s/def ::e #(or (keyword? %)
                (integer? %)
                (symbol? %)))
(s/def ::a #(or (keyword? %)
                (symbol? %)))
(s/def ::v some?)

(s/def ::sexp list?)
(s/def ::bind symbol?)

(s/def ::boolean
  (s/and vector?
         (s/conformer vec vec)
         (s/cat ::bool-op #{:and :or :not}
                ::bool-children (s/+ (s/or ::boolean ::boolean
                                           ::fact ::fact)))))

(s/def ::test (s/tuple #{:test} ::sexp))

(s/def ::accumulator-fn (s/or ::ifn ifn? 
                              ::sexp ::sexp))
(s/def ::accumulator (s/tuple ::bind #{'<-} ::accumulator-fn #{:from} ::fact))

(defn- conditions
  "Transforms a fact conditions from EAV fact form to Clara fact form."
  [eav]
  (let [{::keys [e a v]} eav]
    (remove nil? (list (when (not= e '_)
                         (list '= '(:e this) e))
                       (when (symbol? a)
                         (list '= '(:a this) a))
                       (when (some? v)
                         (list '= '(:v this) v))))))

(defn- fact
  "Transform a fact-eav into a fact-clara"
  [fact-eav]
  (let [{::keys [bind bind-arrow eav]} fact-eav
        eav2nd (second eav)
        a (::a eav2nd)
        type (condp #(%1 %2) a
               nil? :eav/all
               symbol? :eav/all
               keyword? a)]
    (cond-> {::type type
             ::conditions (conditions eav2nd)}
            (some? bind) (assoc ::bind bind)
            (some? bind-arrow) (assoc ::bind-arrow bind-arrow))))

(defn- node
  "If node matches rules transform it, otherwise return it unchanged."
  [node]
  (match/match node
    [::fact-eav fact-eav] [::fact-clara (fact fact-eav)]
    :else node))

(defn- transform
  "Convert the ClaraEAV form containing triplets to plain Clara rule based on 
  the EAV type by walking each node of the input using prewalk and replacing 
  `::fact-eav` nodes with `::fact-clara` nodes."
  [spec form-eav]
  (->> form-eav
       (s/conform spec)
       (walk/prewalk node)
       (s/unform spec)))

(defmacro defrule
  "Like Clara-Rules defrule but with support for EAV fact expressions."
  [name & form-eav]
  `(rules/defrule ~name 
     ~@(transform ::defrule form-eav)))

(defmacro defquery
  "Like Clara-Rules defquery but with support for EAV fact expressions."
  [name & form-eav]
  `(rules/defquery ~name
     ~@(transform ::defquery form-eav)))
