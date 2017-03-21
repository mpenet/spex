(ns qbits.spex.json-schema
  "Trying to fully interpret a spec can be difficult, error prone, so
  we just provide an extended s/def form that provides metadata and
  generate json-schemas from there. It's more work for the user in
  some cases, but it's also more flexible and this doesn't bake in
  conformers into it which is a common approach. That paired with
  ready made json conformers should be composable enough. On the plus
  side, the code is quite minimal.

  TODO: un-unglify some things, make walkers more open (use multimethods)"
  (:require
   [clojure.spec :as s]
   [clojure.spec.specs]
   [qbits.spex.json :as json]))

(def registry
  (atom {::json/string {:type :string}
         ::json/integer {:type :integer :format :int64}
         ::json/long {:type :integer}
         ::json/float {:type :number}
         ::json/double {:type :number :format :double}
         ::json/boolean {:type :boolean}
         ::json/short {:type :number}
         ::json/biginteger {:type :number}
         ::json/bigint {:type :number}
         ::json/set {:type :array :uniqueItems true}
         ::json/keyword {:type :string}
         ::json/date {:type :string :format :date-time}
         ::json/uuid {:type :string :format :uuid}}))

(defn add-meta! [k m]
  (swap! registry update k merge m))

(defmacro def+ [k v & [meta]]
  `(do
     (s/def ~k ~v)
     (add-meta! ~k ~meta)))

(def+ ::age ::json/long {:description "foo"})
(def+ ::name ::json/long {:description "foo"})
(def+ ::person (s/keys :req [::age ::name]))

(s/def ::foo (s/or
              :age ::age
              :name ::name
              :person ::person
              :description ::json/string
              :meta-desc (s/nilable ::json/string)
              :foo (s/keys :req-un [::name ::age])
              :and (s/and ::name (s/keys :req-un [::name ::age]))
              :pl (s/+ ::json/string)
              :st (s/* ::json/string)
              :tup (s/tuple ::json/string ::json/string)
              :map (s/map-of ::json/string ::json/integer)
              :map (s/map-of string? number?)
              :coll (s/coll-of string?)
              :coll (s/coll-of ::person)
              :str string?))

(declare form->json-schema)

(defn emit-keys [form required?]
  (let [[n u] (if required? [:req :req-un]
               [:opt :opt-un])]
    (->> (concat (get form n) (get form u))
         (mapv (comp name second)))))

(defn emit-properties
  [form]
  (let [keys (map second (concat (:req form) (:req-un form)))]
    (reduce
     (fn [m k]
       (assoc m (name k) (get @registry k)))
     {}
     keys)))

(defn emit-pred [pred]
  (case pred
    clojure.core/string?  {:type :string}
    clojure.core/boolean? {:type :boolean}
    clojure.core/number? {:type :number}))

(defn emit-spec
  [[type spec]]
  (case type
    :pred
    (emit-pred spec)
    :spec-key
    (or (@registry spec)
        (form->json-schema (clojure.spec.specs/conform (s/form spec))))))

(defn emit-form [{:keys [s args] :as form}]
  (case s
    (clojure.spec/or clojure.spec/alt)
    {:anyOf (mapv form->json-schema args)}

    clojure.spec/and
    {:allOf (map form->json-schema args)}

    ;; clojure.spec/every
    ;; {:type :array :items (emit-form args)}

    clojure.spec/*
    {:type :array
     :items [(emit-spec args)]}

    clojure.spec/+
    {:type :array
     :minItems 1
     :items (emit-spec args)}

    clojure.spec/tuple
    {:type :array
     :items (mapv emit-spec args)
     :minItems (count args)}

    clojure.spec/keys
    (do
      {:type :object
       :required (emit-keys args true)
       :properties (emit-properties args)})

    clojure.spec/map-of
    {:type :object
     :patternProperties {"*" (-> args :vpred emit-spec)}}

    clojure.spec/coll-of
    {:allOf (emit-spec (:spec args))}

    clojure.spec/nilable
    {:oneOf [{:type :null} (emit-spec args)]}))

(defn emit-tag [{:keys [tag pred]}]
  {tag
   (case (first pred)
     :pred (emit-pred (second pred))
     :form (emit-form (second pred))
     :spec-key (emit-spec pred))})

(defn emit-vec [form]
  (case (first form)
    :form (emit-form (second form))
    :spec-key (emit-spec form)))

(defn emit-map [form]
  (if (:tag form)
    (emit-tag form)))

(defn form->json-schema [form]
  (cond
    (vector? form)
    (emit-vec form)

    (map? form)
    (emit-map form)

    :else form))

(defn json-schema [spec]
  (-> spec s/form clojure.spec.specs/conform form->json-schema))

(clojure.pprint/pprint (json-schema ::foo))
