(ns qbits.spex)

(defmacro rel-ns
  "Creates a relative aliased namespace matching supplied symbol"
  [k]
  `(alias ~k (create-ns (symbol (str *ns* "." (str ~k))))))
