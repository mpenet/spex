(ns qbits.spex.json-schema
  "Trying to fully interpret a spec can be difficult, error prone, so
  we just provide a json-schema spec registry that provides
  metadata + convertion and generate json-schemas from there.
  The registry hold default converters for raw json-schema types, the
  user has to register its specs to map to these types or derive from
  them. Optionally he can also extend them for its own specs to
  provide more metadata.

  It's more work for the user in some cases, but it's also more
  flexible and this doesn't bake in conformers into it. That paired
  with ready made json conformers should be composable enough. On the
  plus side, the code is quite minimal"
  (:require
   [clojure.spec :as s]
   [qbits.spex.specs]))

(defmulti json-schema identity)

(defmacro inherit-spec!
  "Allows to derive from an extending spec, optionally extending the
  json-schema returned with `m` "
  ([spec inherited-json-schema]
   (extend-spec! spec inherited-json-schema nil))
  ([spec inherited-json-schema extras]
   `(defmethod json-schema ~spec [_#]
      (merge (json-schema ~inherited-json-schema) ~extras))))

(defmacro register-spec!
  [spec json-schema-type]
  `(defmethod json-schema ~spec [_#] ~json-schema-type))

(register-spec! ::string {:type :string})
(register-spec! ::integer {:type :integer :format :int64})
(register-spec! ::long {:type :integer :format :int64})
(register-spec! ::float {:type :number})
(register-spec! ::boolean {:type :boolean})
(register-spec! ::set {:type :array :uniqueItems true})
(register-spec! ::map {:type :object})
(register-spec! ::list {:type :array})
(register-spec! ::date {:type :string :format :date-time})
(register-spec! ::uuid {:type :string :format :uuid})
(register-spec! :default nil)
(derive ::keyword ::string)
(derive ::symbol ::string)
(derive ::vector ::list)

(extend-spec! clojure.core/string? ::string)
(extend-spec! clojure.core/boolean? ::boolean)
(extend-spec! clojure.core/number? ::number)
(extend-spec! clojure.core/float? ::float)
(extend-spec! clojure.core/double? ::double)
(extend-spec! clojure.core/number? ::number)
(extend-spec! clojure.core/int? ::integer)
(extend-spec! clojure.core/pos-int? ::integer {:format :int64 :minimum 1})
(extend-spec! clojure.core/neg-int? ::integer {:format :int64 :maximum -1})
(extend-spec! clojure.core/keyword? ::keyword)
(extend-spec! clojure.core/list? ::list)
(extend-spec! clojure.core/vector? ::vector)
(extend-spec! clojure.core/map? ::map)

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
       (assoc m (name k) (json-schema k)))
     {}
     keys)))

(defmulti emit-spec (fn [[type spec]] type))

(defmethod emit-spec :pred
  [[_ spec]]
  (json-schema spec))

(defmethod emit-spec :spec-key
  [[_ spec]]
  (or (json-schema spec)
      (form->json-schema (qbits.spex.specs/conform (s/form spec)))))

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
  (json-schema (second pred)))

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

(defn generate [spec]
  (->> spec s/form qbits.spex.specs/conform form->json-schema))


(do
    (require '[qbits.spex.json :as json])

 (s/def ::age int?)
 (s/def ::name ::json/string)
 (s/def ::description string?)

 (extend-spec! ::age ::long {:description "bla bla"})
 (extend-spec! ::description ::long)
 (extend-spec! ::json/string ::string)
 (extend-spec! ::json/integer ::integer)
 (extend-spec! ::name ::string)


 (s/def ::person (s/keys :req [::age ::name]))

 (s/def ::foo (s/or
               :age ::age
               :name ::name
               :person ::person
               :description ::description
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

 (clojure.pprint/pprint (generate ::foo))
 )
