(ns clojure.spec.specs
  "Lifted/adapted from http://dev.clojure.org/jira/browse/CLJ-2112"
  (:require [clojure.spec :as s]))

;; open spec for spec forms
(defmulti spec-form first)

;; any spec form (spec-form + preds + sets)
(s/def ::spec
  (s/or :set set?
        :pred symbol?
        :spec-key keyword?
        :form (s/multi-spec spec-form (fn [val tag] val))))

(defmethod spec-form 'clojure.core/fn [_]
  (s/cat :f #{'clojure.core/fn}
         :args (s/and vector? #(= 1 (count %)))
         :body (s/* any?)))

;; helper to define a spec for a spec form /foo/ as:
;;  ::/foo/-args - the arg spec, suitable for use in an fdef
;;  ::/foo/-form - the form spec, as returned by form
;;  and register as a method in spec-form
(defmacro ^:private defspec [sym args-spec]
  (let [args-key (keyword "clojure.spec.specs" (str (name sym) "-args"))
        form-key (keyword "clojure.spec.specs" (str (name sym) "-form"))]
    `(do
       (s/def ~args-key ~args-spec)

       (s/def ~form-key
         (s/cat :s #{'~sym}
                :args ~args-spec))

       (defmethod spec-form '~sym [_#] ~form-key))))

(defspec clojure.spec/and (s/* ::spec))
(defspec clojure.spec/or (s/* (s/cat :tag keyword? :pred ::spec)))

 ;;;; Colls/

(s/def ::key (s/or :key qualified-keyword?
                   :and (s/cat :and #{'and} :keys (s/* ::key))
                   :or (s/cat :or #{'or} :keys (s/* ::key))))

(s/def ::req (s/coll-of ::key :kind vector? :into []))
(s/def ::req-un (s/coll-of ::key :kind vector? :into []))
(s/def ::opt (s/coll-of qualified-keyword? :kind vector? :into []))
(s/def ::opt-un (s/coll-of qualified-keyword? :kind vector? :into []))
(s/def ::gen ifn?) ;; OPEN: (s/fspec :args (s/cat)) ? more?

(defspec clojure.spec/keys (s/keys* :opt-un [::req ::req-un ::opt ::opt-un ::gen]))
(defspec clojure.spec/merge (s/* ::spec))
(defspec clojure.spec/multi-spec (s/cat :mm qualified-symbol?
                                        :retag (s/alt :k keyword? :f ifn?)))
(defspec clojure.spec/tuple (s/* ::spec))

(s/def ::kind ifn?) ;; OPEN: should this be ::spec ?
(s/def ::into (s/and coll? empty?))
(s/def ::count nat-int?)
(s/def ::min-count nat-int?)
(s/def ::max-count nat-int?)
(s/def ::distinct boolean?)
(s/def ::conform-keys boolean?)
(s/def ::gen-max nat-int?)

(s/def ::coll-opts
  (s/keys* :opt-un [::kind ::into ::count ::min-count ::max-count ::distinct
                    ::gen-max ::gen]))

(defspec clojure.spec/every
  (s/cat
   :spec ::spec
   :opts ::coll-opts))

(defspec clojure.spec/every-kv
  (s/cat
   :kpred ::spec
   :vpred ::spec
   :opts ::coll-opts))

(defspec clojure.spec/coll-of
  (s/cat
   :spec ::spec
   :opts ::coll-opts))

(defspec clojure.spec/map-of
  (s/cat
   :kpred ::spec
   :vpred ::spec
   :opts ::coll-opts))

;; not very refined but will do for now
(s/def ::conform-fn (s/fspec :args (s/cat :value any?)
                             :ret any?))
(s/def ::unform-fn (s/fspec :args (s/cat :value any?)
                             :ret any?))

 ;;;; regex

(defspec clojure.spec/cat (s/* (s/cat :tag keyword? :spec ::spec)))
(defspec clojure.spec/alt (s/* (s/cat :tag keyword? :spec ::spec)))
(defspec clojure.spec/* ::spec)
(defspec clojure.spec/+ ::spec)
(defspec clojure.spec/? ::spec)
(defspec clojure.spec/& (s/cat :regex ::spec :preds (s/* ::spec)))
(defspec clojure.spec/keys* (s/keys* :opt-un [::req ::req-un ::opt ::opt-un ::gen]))
(defspec clojure.spec/nilable ::spec)
(defspec clojure.spec/conformer (s/cat :conform ::conform
                                       :unform (s/? ::unform)))

(def conform (partial s/conform ::spec))

#_(do
    (s/conform ::spec (s/form (s/spec int?)))
    (s/conform ::spec (s/form (s/spec #{42})))
    (s/conform ::spec (s/form (s/spec #(= % 42))))
    (s/conform ::spec (s/form (s/spec even?)))
    (s/conform ::spec (s/form (s/and int? even?)))
    (s/conform ::spec (s/form (s/or :a int? :b even?)))
    (s/conform ::key :a/b)
    (s/conform ::key '(and :a/b :c/d))
    (s/conform ::key '(or :a/b :c/d))
    (s/conform ::key '(and (or :a/b :c/d) :e/f))
    (s/conform ::spec (s/form (s/keys :req [::foo])))
    (s/conform ::spec (s/form (s/merge (s/keys) (s/keys :req [::foo]))))
    (defmulti ms :tag)
    (s/conform ::spec (s/form (s/multi-spec ms identity)))
    (s/conform ::spec (s/form (s/tuple int? string?)))
    (s/conform ::spec (s/form (s/every int?)))
    (s/conform ::spec (s/form (s/every-kv int? int?)))
    (s/conform ::spec (s/form (s/coll-of int?)))
    (s/explain ::spec (s/form (s/map-of int? int? :conform-keys true))) ;; fails - :kind is fn object
    (s/conform ::spec (s/form (s/cat :a int? :b string?)))
    (s/conform ::spec (s/form (s/alt :a int? :b string?)))
    (s/conform ::spec (s/form (s/* int?)))
    (s/conform ::spec (s/form (s/+ int?)))
    (s/conform ::spec (s/form (s/? int?)))
    (s/conform ::spec (s/form (s/& (s/* int?) #(= (count %) 3))))

    ;; ;; derived
    ;; (s/conform ::spec (s/form (s/keys* :req [::foo])))
    (s/conform ::spec (s/form (s/conformer str int?)))
    (s/conform ::spec (s/form (s/nilable int?)))
    (s/conform ::spec (s/form (s/int-in 0 10)))
    (s/conform ::spec (s/form (s/inst-in #inst "1977" #inst "1978")))
    (s/conform ::spec (s/form (s/double-in :min 0.0 :max 1.0 :infinite? false :NaN? false)))
    )
