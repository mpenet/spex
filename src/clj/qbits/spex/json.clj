(ns qbits.spex.json
  "Validators/Conformers for JSON input data - uses cheshire as type
  convertion model for now. But this should be open via the JSONCodec
  protocol.

  TODO add :gen"
  (:require
   [qbits.spex :as x]
   [clojure.spec :as s]))

(defprotocol ICodec
  (integer-like? [x])
  (float-like? [x])
  (double-like? [x])
  (long-like? [x])
  (short-like? [x])
  (biginteger-like? [x])
  (bigint-like? [x])
  (string-like? [x])
  (keyword-like? [x])
  (symbol-like? [x])
  (set-like? [x]))

(extend-protocol ICodec
  Number
  (string-like? [x] (str x))

  Double
  (double-like? [x] x)

  Long
  (long-like? [x] x)

  String
  (string-like? [x] x)
  (integer-like? [x] (x/try-or-invalid (Integer/parseInt x)))
  (long-like? [x] (x/try-or-invalid (Long/parseLong x)))
  (double-like? [x] (x/try-or-invalid (Double/parseDouble x)))
  (short-like? [x] (x/try-or-invalid (Short/parseShort x)))
  (bigint-like? [x] (x/try-or-invalid (-> x BigInteger. clojure.lang.BigInt/fromBigInteger)))
  (biginteger-like? [x] (x/try-or-invalid (BigInteger. x)))
  (float-like? [x] (x/try-or-invalid (Float/parseFloat x)))
  (keyword-like? [x] (keyword x))
  (symbol-like? [x] (symbol x))

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
  (set-like? [x] :clojure.spec/invalid)
  (keyword-like? [x] :clojure.spec/invalid)
  (symbol-like? [x] :clojure.spec/invalid)

  nil
  (integer-like? [x] :clojure.spec/invalid)
  (float-like? [x] :clojure.spec/invalid)
  (double-like? [x] :clojure.spec/invalid)
  (long-like? [x] :clojure.spec/invalid)
  (short-like? [x] :clojure.spec/invalid)
  (biginteger-like? [x] :clojure.spec/invalid)
  (bigint-like? [x] :clojure.spec/invalid)
  (string-like? [x] :clojure.spec/invalid)
  (set-like? [x] :clojure.spec/invalid)
  (keyword-like? [x] :clojure.spec/invalid)
  (symbol-like? [x] :clojure.spec/invalid))

(s/def ::string (s/conformer string-like?))
(s/def ::integer (s/conformer integer-like?))
(s/def ::float (s/conformer float-like?))
(s/def ::long (s/conformer long-like?))
(s/def ::double (s/conformer double-like?))
(s/def ::short (s/conformer short-like?))
(s/def ::biginteger (s/conformer biginteger-like?))
(s/def ::bigint (s/conformer bigint-like?))
(s/def ::set (s/conformer set-like?))
(s/def ::keyword (s/conformer keyword-like?))
(s/def ::symbol (s/conformer symbol-like?))


(s/def ::n ::integer)
;; (s/def ::s ::string)
;; (s/def ::l ::set)
;; (s/def ::d (s/keys :opt-un [::n ::s ::l]))

(s/exercise ::integer)

;; (s/conform ::d {:l []})
;; (s/conform (::set) #{})
