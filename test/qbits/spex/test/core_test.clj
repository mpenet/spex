(ns qbits.spex.test.core-test
  (:require
   [clojure.test :refer :all]
   [qbits.spex :as sx]
   [clojure.spec.alpha :as s]))

(s/def ::foo string?)
(s/def ::bar string?)

(sx/unregister-meta! ::foo)
(sx/unregister-meta! ::bar)

(deftest test-meta
  (is (= (sx/meta ::foo) nil))

  (sx/reset-meta! ::foo {:bar :baz})
  (is (= (sx/meta ::foo) {:bar :baz}))

  (sx/alter-meta! ::foo assoc :bak :prout)
  (is (= (sx/meta ::foo) {:bak :prout
                          :bar :baz}))


  (derive ::bar ::foo)
  (is (= (sx/meta ::bar) nil))

  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz}))

  (sx/alter-meta! ::bar assoc :1 :2)
  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz
          :1 :2})))

;; (run-tests)
