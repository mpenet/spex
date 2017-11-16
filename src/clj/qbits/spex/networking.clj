(ns qbits.spex.networking
  (:require
   [clojure.string :as str]
   [clojure.test.check.generators :as gen]
   [clojure.spec.alpha :as s]
   [com.gfredericks.test.chuck.generators :as gen']))

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


;; could also try to lift some rx from
;; https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
;; and apply some test.chuck magic on it
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

(s/def ::email
  ;; rx from
  ;; https://github.com/android/platform_frameworks_base/blob/master/core/java/android/util/Patterns.java
  (let [email-rx (re-pattern (str "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}"
                                  "\\@"
                                  "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}"
                                  "("
                                  "\\."
                                  "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}"
                                  ")+"))]
    (letfn [(email? [x] (re-matches email-rx x))
            (gen [] (gen'/string-from-regex email-rx))]
      (s/spec email? :gen gen))))

(s/def ::uri
  ;; http://jmrware.com/articles/2009/uri_regexp/URI_regex.html
  ;; we could make a builder that limit schemes to a predefined set...
  (let [uri-rx (re-pattern "([A-Za-z][A-Za-z0-9+\\-.]*):(?:(//)(?:((?:[A-Za-z0-9\\-._~!$&'()*+,;=:]|%[0-9A-Fa-f]{2})*)@)?((?:\\[(?:(?:(?:(?:[0-9A-Fa-f]{1,4}:){6}|::(?:[0-9A-Fa-f]{1,4}:){5}|(?:[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){4}|(?:(?:[0-9A-Fa-f]{1,4}:){0,1}[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){3}|(?:(?:[0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){2}|(?:(?:[0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})?::[0-9A-Fa-f]{1,4}:|(?:(?:[0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})?::)(?:[0-9A-Fa-f]{1,4}:[0-9A-Fa-f]{1,4}|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))|(?:(?:[0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})?::[0-9A-Fa-f]{1,4}|(?:(?:[0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})?::)|[Vv][0-9A-Fa-f]+\\.[A-Za-z0-9\\-._~!$&'()*+,;=:]+)\\]|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|(?:[A-Za-z0-9\\-._~!$&'()*+,;=]|%[0-9A-Fa-f]{2})*))(?::([0-9]*))?((?:/(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*)|/((?:(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})+(?:/(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*)?)|((?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})+(?:/(?:[A-Za-z0-9\\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*)|)(?:\\?((?:[A-Za-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9A-Fa-f]{2})*))?(?:\\#((?:[A-Za-z0-9\\-._~!$&'()*+,;=:@/?]|%[0-9A-Fa-f]{2})*))?")]
    (letfn [(uri? [x] (re-matches uri-rx x))
            (gen [] (gen'/string-from-regex uri-rx))]
      (s/spec uri? :gen gen))))

(s/def ::url
  (s/spec #(re-matches #"(?i)^https?:\/\/\S+" %)
          :gen (constantly (gen/fmap #(str "http://" % ".com")
                                     (gen/uuid)))))
