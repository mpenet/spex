(ns qbits.spex
  (:refer-clojure :exclude [meta])
  (:require
   [clojure.spec.alpha :as s]))

(defmacro rel-ns
  "Creates a relative aliased namespace matching supplied symbol"
  [k]
  `(alias ~k (create-ns (symbol (str *ns* "." (str ~k))))))

;; The following only works only for registered specs
(s/def ::metadata-registry-val (s/map-of qualified-keyword? any?))

(defonce metadata-registry (atom {}))

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
        :ret ::metadata-registry-val)
(defn unregister-meta!
  "Unregister meta data for a spec"
  [k]
  (swap! metadata-registry dissoc k))

(s/fdef with-doc
        :args (s/cat :k qualified-keyword?
                     :doc string?)
        :ret ::metadata-registry-val)
(defn with-doc
  "Add doc metadata on a registered spec"
  [k doc]
  (vary-meta! k assoc :doc doc))
