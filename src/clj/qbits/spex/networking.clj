(ns qbits.spex.networking
  (:require
   [clojure.string :as str]
   [clojure.test.check.generators :as gen]
   [clojure.spec :as s]))

(s/def ::port (s/and nat-int? #(s/int-in-range? 1 65535 %)))

(s/def ::hostname
  (letfn [(hostname? [x]
            (re-matches #"^(?=.{1,255}$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\.?$" x))
          (gen []
            (gen/fmap #(str/join (if (pos? (rand-int 2)) "" ".") %)
                      (gen/vector
                       (gen/fmap #(apply str %)
                                 (gen/vector gen/char-alphanumeric 2 10))
                       1 10)))]
    (s/spec hostname? :gen gen)))

(s/def ::ip
  (letfn [(ip? [x]
            (let [segments (str/split x #"\.")]
              (and (= 4 (count segments))
                   (every? #(try
                              (let [i (Integer/parseInt %)]
                                (and (integer? i) (<= i 255)))
                              (catch NumberFormatException e
                                false))
                           segments))))
          (gen []
            (gen/fmap #(str/join "." %)
                      (gen/vector (gen/choose 0 255) 4)))]
    (s/spec ip? :gen gen)))
