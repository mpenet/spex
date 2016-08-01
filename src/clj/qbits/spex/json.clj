(ns qbits.spex.json
  "Couple of specs to convert/validate from json->clj"
  (:require
   [qbits.spex :as x]
   [clojure.spec :as s]))

;; we could have a protocol with 1 fn per *-like to make it more "open"

(defprotocol JSONCodec
  (integer-like? [x])
  (float-like? [x])
  (double-like? [x])
  (long-like? [x])
  (short-like? [x])
  (biginteger-like? [x])
  (bigint-like? [x])
  (string-like? [x])
  (set-like? [x]))

(extend-protocol JSONCodec
  Number
  (string-like? [x] (str x))

  String
  (string-like? [x] x)
  (integer-like? [x] (x/try-or-invalid (Integer/parseInt x)))
  (long-like? [x] (x/try-or-invalid (Long/parseLong x)))
  (double-like? [x] (x/try-or-invalid (Double/parseDouble x)))
  (short-like? [x] (x/try-or-invalid (Short/parseShort x)))
  (bigint-like? [x] (x/try-or-invalid (-> x BigInteger. clojure.lang.BigInt/fromBigInteger)))
  (biginteger-like? [x] (x/try-or-invalid (BigInteger. x)))
  (float-like? [x] (x/try-or-invalid (Float/parseFloat x)))

  clojure.lang.IPersistentCollection
  (set-like? [x] (set x))

  Object
  (integer-like? [x] :clojure.spec/invalid)
  (float-like? [x] :clojure.spec/invalid)
  (double-like? [x] :clojure.spec/invalid)
  (long-like? [x] :clojure.spec/invalid)
  (short-like? [x] :clojure.spec/invalid)
  (biginteger-like? [x] :clojure.spec/invalid)
  (bigint-like? [x] :clojure.spec/invalid)
  (string-like? [x] :clojure.spec/invalid)
  (set-like? [x] :clojure.spec/invalid))

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
