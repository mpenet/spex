# spex
<!-- [![Build Status](https://secure.travis-ci.org/mpenet/spex.png?branch=master)](http://travis-ci.org/mpenet/spex) -->

Small utility/extension library for `clojure.spec`.

Subject to changes/breakage. Use at own risk.

At the moment it does only 3 things:

assuming

``` clojure
(require '[qbits.spex :as spex])
```

* adds a sugar to create namespaces within a ns
  If you are in the user namespace
  `(spex/rel-ns 'foo.bar)` would create user.foo.bar

* add `def-derived` which creates a keyword hierarchy behind the
  scenes (the hierarchy is internal/scoped to spec/ so no risk of
  cluttering the global one).

  ```clj
  (def-derived ::foo ::bar)
  ```
  equivalent to
  ```clj
  (s/def ::foo ::bar)
  (spex/derive ::foo ::bar)
  ```

  but also works with more arguments in that case we assume you're working with maps

  ```clj
  (s/def-derive ::foo ::bar ::baz ...)
  ```
  equivalent to
  ```clj
  (s/def ::foo (s/merge ::bar ::baz))
  (spex/derive ::foo ::bar)
  (spex/derive ::foo ::baz)
  ```

* adds a metadata registry for registered specs, it currently supports
  variants of `vary-meta!`, `with-meta!`, `meta`, adds
  `unregister-meta!` and `with-doc`.

  you can then write code like:
  ```clj
  (-> (s/def ::foo string?)
      (spex/vary-meta! {:something :you-need}))

   ;; and retrieve the values with spex/meta
  (spex/meta ::foo) => {:something :you-need}
  ```

    Once we have this hierarchy in place we can integrate this into
    metadata retrieval

  ```clj
  (spex/def-derived ::bar ::foo)

  (spex/meta ::bar) => nil

  ;; register meta at ::bar level
  (spex/vary-meta! ::bar {:another :key})

  ;; just the meta of ::bar
  (spex/meta ::bar) => {:another :key}

  ;; retrieve the meta for ::bar but also all its ancestors if you pass true to spex/meta
  ;; merged meta of ::foo and ::bar
  (spex/meta ::bar true) => {:something :you-need, :another :key}
  ```

  and `spex/with-doc` is just sugar on top of all this to add docstrings to specs

  ```clj
  (with-doc ::foo "bla bla bla")
  ```

  All the functions that mutate the metadata of a spec return the spec
  key, that makes chaining easier:

  ```clojure
  (-> (s/def ::foo string?)
      (spex/vary-meta! {:something :you-need})
      (cond->
        something?
        (spex/vary-meta! {:something-else :you-might-need})))
  ```
  The internal hierarchy is queriable just like a normal clojure one
  you can use `spex/isa?` `spex/descendants` `spex/ancestors`
  `spex/parents` `spex/derive` `spex/underive`, which are just
  partially applied functions over the same functions in core with our
  own hierarchy `spex/spec-hierarchy`.


  ```clojure
     (s/def ::port (int-in-range? 1 65535))
     (s/def-derived ::redis-port ::port)

     (spex/isa? ::redis-port ::port) => true

     (s/def-derived ::cassandra-port ::port)

     ;; list all things ::port
     (spex/descendants ::port) => #{::redis-port ::cassandra-port}))

  ```

  This only works for aliases obviously.

## Installation

spex is [available on Clojars](https://clojars.org/cc.qbits/spex).

[![Clojars Project](https://img.shields.io/clojars/v/cc.qbits/spex.svg)](https://clojars.org/cc.qbits/spex)

## License

Copyright © 2016 [Max Penet](http://twitter.com/mpenet)

Distributed under the
[Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html),
the same as Clojure.
