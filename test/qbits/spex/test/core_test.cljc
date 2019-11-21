(ns qbits.spex.test.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [qbits.spex :as sx]
   [clojure.spec.alpha :as s]))

(s/def ::foo string?)

(deftest test-meta
  (is (= (sx/meta ::foo) nil))

  (sx/with-meta! ::foo {:bar :baz})
  (is (= (sx/meta ::foo) {:bar :baz}))

  (sx/vary-meta! ::foo assoc :bak :prout)
  (is (= (sx/meta ::foo) {:bak :prout
                          :bar :baz}))

  (sx/def-derived ::bar ::foo)

  (is (= (sx/meta ::bar) nil))
  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz}))

  (sx/vary-meta! ::bar assoc :1 :2)
  (is (= (sx/meta ::bar true)
         {:bak :prout
          :bar :baz
          :1 :2})))

(deftest test-hierachy
  (s/def ::bak string?)
  (is (sx/isa? ::bar ::foo))
  (is (not (sx/isa? ::foo ::baz)))

  (sx/def-derived ::bak ::bar)
  (is (sx/isa? ::bak ::foo))
  (is (= (sx/ancestors ::bak) #{::bar ::foo}))
  (is (= (sx/descendants ::foo) #{::bar ::bak}))
  (is (= (sx/parents ::foo) nil))
  (is (= (sx/parents ::bar) #{::foo}))

  (sx/underive ::baz ::bar)
  (is (= (sx/ancestors ::baz) nil)))

(deftest test-merged
  (s/def ::a (s/keys))
  (s/def ::b (s/keys))
  (sx/def-merged ::m [::a ::b])
  (is (= #{::a ::b} (sx/ancestors ::m))))
