(ns qbits.spex.test.core-test
  (:require
   [clojure.test :refer :all]
   [qbits.spex :as sx]
   [clojure.spec.alpha :as s]))

(s/def ::foo string?)

(sx/unregister-meta! ::foo)
(sx/unregister-meta! ::bar)

(deftest test-meta
  (is (= (sx/meta ::foo) nil))

  (sx/with-meta! ::foo {:bar :baz})
  (is (= (sx/meta ::foo) {:bar :baz}))

  (sx/vary-meta! ::foo assoc :bak :prout)
  (is (= (sx/meta ::foo) {:bak :prout
                          :bar :baz}))

  (s/def ::bar ::foo)

  (is (= (sx/meta ::bar) nil))
  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz}))

  (sx/vary-meta! ::bar assoc :1 :2)
  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz
          :1 :2})))

(deftest tst-hierachy
  (s/def ::bak string?)
  (is (sx/isa? ::bar ::foo))
  (is (not (sx/isa? ::foo ::baz)))

  (s/def ::bak ::bar)
  (is (sx/isa? ::bak ::foo))
  (is (= (sx/ancestors ::bak) #{::bar ::foo}))
  (is (= (sx/descendants ::foo) #{::bar ::bak}))
  (is (= (sx/parents ::foo) nil))
  (is (= (sx/parents ::bar) #{::foo}))

  (sx/underive ::baz ::bar)
  (is (= (sx/ancestors ::baz) nil)))
