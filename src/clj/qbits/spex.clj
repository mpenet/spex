(ns qbits.spex
  (:require [clojure.spec.alpha :as s]))

(defn instance-of
  "Partially applied version of clojure.core/instance?"
  [k]
  #(instance? k %))

(defn satisfies [p]
  #(satisfies? p %))

(defn ns-as
  "Creates a namespace 'n' (if non existant) and then aliases it to 'a'"
  [n a]
  (create-ns n)
  (alias a n))

(defmacro try-or-invalid
  [& body]
  `(try
     ~@body
     (catch java.lang.Exception e#
       :clojure.spec.alpha/invalid)))

(defn default [spec default]
  (s/conformer
   #(-> spec s/nilable (s/conform %) (or default))))

(defmacro rel-ns
  "Creates a relative aliased namespace matching supplied symbol"
  [k]
  `(alias ~k (create-ns (symbol (str *ns* "." (str ~k))))))
