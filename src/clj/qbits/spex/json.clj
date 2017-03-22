(ns qbits.spex.json
  "Validators/Conformers for JSON input data - uses cheshire as type
  convertion model for now. But this should be open via the ICodec
  protocol.
  todo: tests!"
  (:require
   [qbits.spex :as x]
   [clojure.spec :as s]
   [clojure.string :as str]
   [clojure.test.check.generators :as gen])
  (:import (java.util.Base64$Decoder)))


(defprotocol ICodec
  (json->integer [x])
  (json->float [x])
  (json->double [x])
  (json->long [x])
  (json->short [x])
  (json->biginteger [x])
  (json->bigint [x])
  (json->string [x])
  (json->keyword [x])
  (json->symbol [x])
  (json->set [x])
  (json->map [x])
  (json->date [x])
  (json->uuid [x])
  (json->byte [x])
  (json->binary [x])
  (json->boolean [x]))

(defn conformer
  [f]
  (s/conformer (fn [x] (x/try-or-invalid (f x)))))

;; TODO make this reifiable?
(extend-protocol ICodec
  Number
  (json->string [x] (str x))
  (json->boolean [x] (= x 1))

  Long
  (json->long [x] x)
  (json->integer [x] (int x))
  (json->float [x] (float x))
  (json->double [x] (double x))
  (json->short [x] (short x))
  (json->string [x] (str x))
  (json->date [x] (java.util.Date. x))

  Integer
  (json->integer [x] x)

  Float
  (json->float [x] x)

  Short
  (json->short [x] x)

  Double
  (json->double [x] x)
  (json->string [x] (str x))

  BigInteger
  (json->biginteger [x] x)

  clojure.lang.BigInt
  (json->bigint [x] x)

  String
  (json->string [x] x)
  (json->integer [x] (Integer/parseInt x))
  (json->long [x] (Long/parseLong x))
  (json->double [x] (Double/parseDouble x))
  (json->short [x] (Short/parseShort x))
  (json->bigint [x] (-> x BigInteger. clojure.lang.BigInt/fromBigInteger))
  (json->biginteger [x] (BigInteger. x))
  (json->float [x] (Float/parseFloat x))
  (json->keyword [x] (keyword x))
  (json->symbol [x] (symbol x))
  (json->uuid [x] (java.util.UUID/fromString x))
  (json->byte [x] (-> (java.util.Base64/getDecoder) (.decode x)))
  (json->binary [x] x)
  (json->boolean [x] (Boolean/parseBoolean x))

  Boolean
  (json->boolean [x] x)

  clojure.lang.Keyword
  (json->keyword [x] x)

  clojure.lang.Symbol
  (json->symbol [x] x)

  java.util.UUID
  (json->uuid [x] x)

  java.util.Date
  (json->date [x] x)

  clojure.lang.IPersistentCollection
  (json->set [x] (set x))

  clojure.lang.IPersistentMap
  (json->map [x] x))

(def nat-str-gen (gen/one-of [gen/nat (gen/fmap str gen/nat)]))

(s/def ::string
  (s/spec (conformer json->string)
          :gen (constantly (gen/one-of [gen/string
                                        gen/int
                                        gen/double]))))
(s/def ::integer
  (s/spec (conformer json->integer)
          :gen (constantly (gen/one-of [nat-str-gen gen/int]))))

(s/def ::float
  (s/spec (conformer json->float)
          :gen (constantly nat-str-gen)))

(s/def ::long
  (s/spec (conformer json->long)
          :gen (constantly nat-str-gen)))

(s/def ::double
  (s/spec (conformer json->double)
          :gen (constantly (gen/one-of [nat-str-gen gen/double]))))

(s/def ::short
  (s/spec (conformer json->short)
          :gen (constantly nat-str-gen)))

(s/def ::biginteger
  (s/spec (conformer json->biginteger)
          :gen (constantly nat-str-gen)))

(s/def ::bigint
  (s/spec (conformer json->bigint)
          :gen (constantly nat-str-gen)))

(s/def ::set
  (s/spec (conformer json->set)
          :gen (constantly (gen/one-of [(gen/vector gen/any)
                                        (gen/list gen/any)
                                        (gen/set gen/any)
                                        (gen/map gen/any gen/any)]))))

(s/def ::keyword
  (s/spec (conformer json->keyword)
          :gen (constantly (gen/one-of [gen/string gen/keyword]))))

(s/def ::date
  (s/spec (conformer json->date)
          :gen (constantly (gen/one-of [gen/pos-int (gen/fmap #(java.util.Date %)
                                                              gen/pos-int)]))))

(s/def ::uuid
  (s/spec (conformer json->uuid)
          :gen (constantly (gen/one-of [(gen/fmap str gen/uuid) gen/uuid]))))

(s/def ::boolean
  (s/spec (conformer json->boolean)
          :gen (constantly (gen/one-of [gen/string gen/boolean]))))

;; some extra stuff (arguably useful, mostly for ring.params)
(s/def ::comma-separated-string
  (s/spec
   (conformer (fn [x] (and (string? x)
                           (when (not-empty x)
                             (some->> (str/split x #"\s*,\s*")
                                      (into #{} (remove #(= % ""))))))))))

(s/def ::space-separated-string
  (s/spec
   (conformer
    (fn [x] (and (string? x)
                 (when (not-empty x)
                   (set (re-seq #"\S+" x))))))))
