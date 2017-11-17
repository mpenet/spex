(ns qbits.spex.net
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

(defmacro uri-spec-for-scheme
  "lifted from android code again"
  [kw schemes]
  (let [user-info
        (str "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
             "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
             "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@")
        port-number "\\:\\d{1,5}"
        ucs-char (str "["
                      "\u00A0-\uD7FF"
                      "\uF900-\uFDCF"
                      "\uFDF0-\uFFEF"
                      "\uD800\uDC00-\uD83F\uDFFD"
                      "\uD840\uDC00-\uD87F\uDFFD"
                      "\uD880\uDC00-\uD8BF\uDFFD"
                      "\uD8C0\uDC00-\uD8FF\uDFFD"
                      "\uD900\uDC00-\uD93F\uDFFD"
                      "\uD940\uDC00-\uD97F\uDFFD"
                      "\uD980\uDC00-\uD9BF\uDFFD"
                      "\uD9C0\uDC00-\uD9FF\uDFFD"
                      "\uDA00\uDC00-\uDA3F\uDFFD"
                      "\uDA40\uDC00-\uDA7F\uDFFD"
                      "\uDA80\uDC00-\uDABF\uDFFD"
                      "\uDAC0\uDC00-\uDAFF\uDFFD"
                      "\uDB00\uDC00-\uDB3F\uDFFD"
                      "\uDB44\uDC00-\uDB7F\uDFFD"
                      "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]")
        label-char (str "a-zA-Z0-9" ucs-char)
        path-and-query  (str "[/\\?](?:(?:["
                             label-char
                             ";/\\?:@&=#~"
                             "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*")
        word-boundary "(?:\\b|$|^)"
        ip-address (str "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                        "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                        "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                        "|[1-9][0-9]|[0-9]))")
        iri-label  (str "["  label-char "](?:[" label-char "_\\-]{0,61}[" label-char "]){0,1}")
        punycode-tld "xn\\-\\-[\\w\\-]{0,58}\\w"
        tld-char (str "a-zA-Z" ucs-char)
        tld (str "(" punycode-tld "|" "[" tld-char "]{2,63})")
        hostname (str "(" iri-label ")+(\\." tld ")?")
        domain-name (str hostname "|" ip-address) ]
    `(s/def ~kw
       (let [rx# (re-pattern ~(str "("
                                   "("
                                   "(?:" "(?i:" (str/join "|" schemes) ")://" "(?:"  user-info  ")?"  ")?"
                                   "(?:"  domain-name  ")"
                                   "(?:"  port-number  ")?"
                                   ")"
                                   "("  path-and-query  ")?"
                                   word-boundary
                                   ")"
                                   ))]
         (s/spec #(re-matches rx# %)
                 :gen (constantly
                       (gen/fmap (fn [[scheme# path#]] (str scheme# "://" path#))
                                 (gen/tuple
                                  (gen/elements ~schemes)
                                  gen/string-alphanumeric))))))))

(uri-spec-for-scheme ::http ["http" "https"])
(uri-spec-for-scheme ::ws ["ws" "wss"])
(uri-spec-for-scheme ::redis ["redis"])
