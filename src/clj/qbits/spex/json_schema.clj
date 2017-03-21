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
  `(let [m# ~meta]
     (s/def ~k ~v)
     (when m# (add-meta! ~k m#))))

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
              :coll1 (s/coll-of string?)
              :coll2 (s/coll-of ::person)
              :str string?))

(declare form->json-schema)

(defn emit-keys [form required?]
  (let [[n u] (if required? [:req :req-un]
               [:opt :opt-un])]
    (->> (concat (get form n) (get form u))
         (mapv (comp name second)))))

(defn emit-properties
  [form]
  (let [keys (map second
                  (concat (:req form)
                          (:req-un form)))]
    (reduce
     (fn [m k]
       (assoc m (name k) (get @registry k)))
     {}
     keys)))

(defmulti emit-pred identity)
(defmethod emit-pred 'clojure.core/string?
  [_] {:type :string})

(defmethod emit-pred 'clojure.core/boolean?
  [_] {:type :boolean})

(defmethod emit-pred 'clojure.core/number?
  [_] {:type :number})


(defmulti emit-spec (fn [[type spec]] type))

(defmethod emit-spec :pred
  [[_ spec]]
  (emit-pred spec))

(defmethod emit-spec :spec-key
  [[_ spec]]
  (or (@registry spec)
      (form->json-schema (clojure.spec.specs/conform (s/form spec)))))

(defmulti emit-form :s)

(defmethod emit-form 'clojure.spec/or
  [{:keys [args]}]
  {:anyOf (mapv form->json-schema args)})

(defmethod emit-form 'clojure.spec/alt
  [{:keys [args]}]
  {:anyOf (mapv form->json-schema args)})

(defmethod emit-form 'clojure.spec/and
  [{:keys [args]}]
  {:allOf (mapv form->json-schema args)})

(defmethod emit-form 'clojure.spec/*
  [{:keys [args]}]
  {:type :array
   :items [(emit-spec args)]})

(defmethod emit-form 'clojure.spec/+
  [{:keys [args]}]
  {:type :array
   :minItems 1
   :items (emit-spec args)})

(defmethod emit-form 'clojure.spec/tuple
  [{:keys [args]}]
  {:type :array
   :items (mapv emit-spec args)
   :minItems (count args)})

(defmethod emit-form 'clojure.spec/keys
  [{:keys [args]}]
  {:type :object
   :required (emit-keys args true)
   :properties (emit-properties args)})

(defmethod emit-form 'clojure.spec/map-of
  [{:keys [args]}]
  {:type :object
   :patternProperties {"*" (-> args :vpred emit-spec)}})

(defmethod emit-form 'clojure.spec/coll-of
  [{:keys [args]}]
  {:allOf (emit-spec (:spec args))})

(defmethod emit-form 'clojure.spec/nilable
  [{:keys [args]}]
  {:oneOf [{:type :null} (emit-spec args)]})

(defmulti emit-tag (fn [{:keys [pred]}] (first pred)))

(defmethod emit-tag :pred
  [{:keys [tag pred]}]
  (emit-pred (second pred)))

(defmethod emit-tag :form
  [{:keys [tag pred]}]
  (emit-form (second pred)))

(defmethod emit-tag :spec-key
  [{:keys [tag pred]}]
  (emit-spec pred))

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
