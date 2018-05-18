(ns qbits.spex
  (:refer-clojure
   :exclude [meta isa? parents ancestors derive descendants underive])
  (:require
   [clojure.spec.alpha :as s]))

(defmacro rel-ns
  "Creates a relative aliased namespace matching supplied symbol"
  [k]
  `(alias ~k (create-ns (symbol (str *ns* "." (str ~k))))))

;; The following only works only for registered specs
(s/def ::metadata-registry-val (s/map-of qualified-keyword? any?))

(defonce metadata-registry (atom {}))
(defonce spec-hierarchy (atom (make-hierarchy)))

(s/fdef derive
        :args (s/cat :tag qualified-keyword?
                     :parent qualified-keyword?))
(defn derive
  "Like clojure.core/derive but scoped on our spec hierarchy"
  [tag parent]
  (swap! spec-hierarchy
         clojure.core/derive tag parent))

(s/fdef underive
        :args (s/cat :tag qualified-keyword?
                     :parent qualified-keyword?))
(defn underive
  "Like clojure.core/underive but scoped on our spec hierarchy"
  [tag parent]
  (swap! spec-hierarchy
         clojure.core/underive tag parent))

(s/fdef isa?
        :args (s/cat :child qualified-keyword?
                     :parent qualified-keyword?))
(defn isa?
  "Like clojure.core/isa? but scoped on our spec hierarchy"
  [child parent]
  (clojure.core/isa? @spec-hierarchy child parent))

(s/fdef parents
        :args (s/cat :tag qualified-keyword?))
(defn parents
  "Like clojure.core/parents but scoped on our spec hierarchy"
  [tag]
  (clojure.core/parents @spec-hierarchy tag))

(s/fdef ancestors
        :args (s/cat :tag qualified-keyword?))
(defn ancestors
  "Like clojure.core/ancestors but scoped on our spec hierarchy"
  [tag]
  (clojure.core/ancestors @spec-hierarchy tag))

(s/fdef descendants
        :args (s/cat :tag qualified-keyword?))
(defn descendants
  "Like clojure.core/descendants but scoped on our spec hierarchy"
  [tag]
  (clojure.core/descendants @spec-hierarchy tag))

(s/fdef vary-meta!
        :args (s/cat :k qualified-keyword?
                     :f ifn?
                     :args (s/* any?))
        :ret qualified-keyword?)
(defn vary-meta!
  "Like clojure.core/vary-meta but for registered specs, mutates the
  meta in place, return the keyword spec"
  [k f & args]
  (swap! metadata-registry
         #(update % k
                  (fn [m]
                    (apply f m args))))
  k)

(s/fdef with-meta!
        :args (s/cat :k qualified-keyword?
                     :meta any?)
        :ret ::metadata-registry-val)
(defn with-meta!
  "Like clojure.core/with-meta but for registered specs, mutates the
  meta in place, return the keyword spec"
  [k m]
  (swap! metadata-registry
         #(assoc % k m))
  k)

(s/fdef meta
        :args (s/cat :k qualified-keyword?
                     :merge-with-ancestors (s/? boolean?))
        :ret any?)
(defn meta
  "Like clojure.core/meta but for registered specs.
  If merge-with-ancestors? is set to true it will merge with the
  metadata from all parents (top to bottom)"
  ([k merge-with-ancestors?]
   (if merge-with-ancestors?
     (let [m @metadata-registry]
       (reduce
        (fn [m' k]
          (merge m' (get m k)))
        {}
        (conj (vec (ancestors k)) k)))
     (get @metadata-registry k)))

  ([k]
   (meta k false)))

(s/fdef unregister-meta!
        :args (s/cat :k qualified-keyword?)
        :ret qualified-keyword?)
(defn unregister-meta!
  "Unregister meta data for a spec"
  [k]
  (swap! metadata-registry dissoc k)
  k)

(s/fdef with-doc
        :args (s/cat :k qualified-keyword?
                     :doc string?)
        :ret qualified-keyword?)
(defn with-doc
  "Add doc metadata on a registered spec"
  [k doc]
  (vary-meta! k assoc :doc doc))

(s/fdef doc
        :args (s/cat :k qualified-keyword?)
        :ret (s/nilable string?))
(defn doc
  "Returns doc associated with spec"
  [k]
  (some-> (meta k) :doc))

(s/fdef def-derived
        :args (s/cat :k qualified-keyword?
                     :parents (s/+ any?))) ;; refine
(defmacro def-derived
  "2 arg arity will define a new spec such that (s/def ::k ::parent) and
  define a relationship between the 2 with spex/derive such
  that: (spec/isa? k parent) => true.
  3 arg arity will define the same relationship but instead of
  creating a simple spec alias it will create a new spec such that
  (s/def ::k (s/merge ::parent [specs...]).
  Parents derivation only works between registered specs."
  ([k & parents]
   (let [[parent & more] parents]
     `(do
        (s/def ~k
         ~(if more
            `(s/merge ~@parents)
            parent))
        ~@(for [p parents
                ;; we can only derive from registered specs
                :when (qualified-keyword? p)]
            `(derive ~k ~p))
        ~k))))
