(ns qbits.spex.json
  "Couple of specs to convert/validate from json->clj"
  (:require
   [qbits.spex :as x]
   [clojure.spec :as s]))

(defn integer-like? [x]
  (cond
    (integer? x) x
    (string? x) (x/try-or-invalid (Integer/parseInt x))
    :else :clojure.spec/invalid))

(defn float-like? [x]
  (cond
    (float? x) x
    (string? x) (x/try-or-invalid (Float/parseFloat x))
    :else :clojure.spec/invalid))

(defn double-like? [x]
  (cond
    (double? x) x
    (string? x) (x/try-or-invalid (Double/parseDouble x))
    :else :clojure.spec/invalid))

(defn long-like? [x]
  (cond
    (instance? Long x) x
    (string? x)
    (x/try-or-invalid (Long/parseLong x))
    :else :clojure.spec/invalid))

(defn short-like? [x]
  (cond
    (instance? Short x) x
    (string? x)
    (x/try-or-invalid (Short/parseShort x))
    :else :clojure.spec/invalid))

(defn biginteger-like? [x]
  (cond
    (instance? BigInteger x) x
    (string? x) (x/try-or-invalid (BigInteger. ^String x))
    :else :clojure.spec/invalid))

(defn bigint-like? [x]
  (cond
    (instance? BigInteger x) x
    (string? x) (x/try-or-invalid
                 (clojure.lang.BigInt/fromBigInteger (BigInteger. ^String x)))
    :else :clojure.spec/invalid))

(defn string-like? [x]
  ;; what comes in as js string/number can be coerced to clj string
  (cond (string? x) x
        (number? x) (str x)
        :else :clojure.spec/invalid))

(defn set-like? [x]
  (if (coll? x) (set x)
      :clojure.spec/invalid))

(s/def ::string (s/conformer string-like?))
(s/def ::integer (s/conformer integer-like?))
(s/def ::float (s/conformer float-like?))
(s/def ::long (s/conformer long-like?))
(s/def ::double (s/conformer double-like?))
(s/def ::short (s/conformer short-like?))
(s/def ::biginteger (s/conformer biginteger-like?))
(s/def ::bigint (s/conformer bigint-like?))
(s/def ::set (s/conformer set-like?))

;; (s/def ::n ::integer)
;; (s/def ::s ::string)
;; (s/def ::l ::set)
;; (s/def ::d (s/keys :opt-un [::n ::s ::l]))

;; (s/conform ::d {:n "12345"})
;; (s/conform ::set [1 2 3])
