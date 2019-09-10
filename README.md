# spex
[![cljdoc badge](https://cljdoc.xyz/badge/cc.qbits/spex)](https://cljdoc.xyz/d/cc.qbits/spex/CURRENT)

Small utility/extension library for `clojure.spec`.

Subject to changes/breakage. Use at own risk.

At the moment it does only 3 things:

assuming

``` clojure
(require '[qbits.spex :as spex])
```
* add `def-derived` which creates a keyword hierarchy behind the
  scenes (the hierarchy is internal/scoped to spex/ so no risk of
  cluttering the global one)

  ```clj
  (s/def ::foo string?)
  (spex/def-derived ::bar ::foo)
  ```
  equivalent to:
  ```clj
  (s/def ::foo string?)
  (s/def ::bar ::foo)
  (spex/derive ::bar ::foo)
  ```

  but that also works with maps, here foo will derive from ::baz and ::bar

  ```clj
  (spex/def-merged ::foo [::bar ::baz])
  ```
  equivalent to:
  ```clj
  (s/def ::foo (s/merge ::bar ::baz))
  (spex/derive ::foo ::bar)
  (spex/derive ::foo ::baz)
  ```

  So why do that? well you can then inspect the hierarchy, which can
  be handy:

  ``` clj
  (s/def-merged ::foo [::bar ::baz])

  (spex/ancestors ::foo) => #{::bar ::baz}
  (spex/isa? ::foo ::bar) => true
  (spex/isa? ::foo ::baz) => true
  ```

* adds a metadata registry for registered specs, it currently supports
  variants of `vary-meta!`, `with-meta!`, `meta`, adds
  `unregister-meta!` and `with-doc`.

  you can then write code like:
  ```clj
  (-> (s/def ::foo string?)
      (spex/vary-meta! assoc :something :you-need))

   ;; and retrieve the values with spex/meta
  (spex/meta ::foo) => {:something :you-need}
  ```

  Since we have this hierarchy in place we can integrate this into
  metadata retrieval. Told you it could be useful :)

  ```clj
  (spex/def-derived ::bar ::foo)

  (spex/meta ::bar) => nil

  ;; remember we have meta data on :foo already, such that:
  (spex/meta ::foo) => {:something :you-need}

  ;; register meta at ::bar level
  (spex/vary-meta! ::bar assoc :another :key)

  ;; just the meta of ::bar
  (spex/meta ::bar) => {:another :key}

  ;; retrieve the meta for ::bar but also all its ancestors if you pass true to spex/meta
  (spex/meta ::bar true) => {:something :you-need, :another :key}
  ```

  and `spex/with-doc` is just sugar on top of all this to add docstrings to specs

  ```clj
  (spex/with-doc ::foo "bla bla bla")

  (s/doc ::foo) => "bla bla bla"
  ```

  All the functions that mutate the metadata of a spec return the spec
  key, that makes chaining easier, same goes for `spex/def-derived`:

  ```clojure
  (-> (s/def ::foo string?)
      (spex/vary-meta! assoc :something :you-need)
      (cond->
        something?
        (spex/vary-meta! assoc :something-else :you-might-need)))
  ```
  The internal hierarchy is queriable just like the global keyword hierarchy,
  you can use `spex/isa?` `spex/descendants` `spex/ancestors`
  `spex/parents` `spex/derive` `spex/underive`, which are just
  partially applied functions over the same functions in core with our
  own internal hierarchy `spex/spec-hierarchy`.


  ```clojure
     (s/def ::port (int-in-range? 1 65535))
     (spex/def-derived ::redis-port ::port)

     (spex/isa? ::redis-port ::port) => true

     (spex/def-derived ::cassandra-port ::port)

     ;; list all things ::port
     (spex/descendants ::port) => #{::redis-port ::cassandra-port}))

  ```

  This only works for aliases obviously.

* adds a sugar to create namespaces within a ns. Ex: if you are in
  the user namespace `(spex/rel-ns 'foo.bar)` would create
  user.foo.bar

## Installation

spex is [available on Clojars](https://clojars.org/cc.qbits/spex).

[![Clojars Project](https://img.shields.io/clojars/v/cc.qbits/spex.svg)](https://clojars.org/cc.qbits/spex)

## License

Copyright © 2016 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
