(ns scicloj.kindly.gen
  "The Kindly specification is encapsulated in kinds.edn
  This code generates the namespaces of the Kindly library from kinds.edn
  so that it may be more conveniently consumed by users"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn read-kinds []
  (-> (io/resource "kinds.edn")
      (slurp)
      (edn/read-string)))

(defn escape [s]
  (str/replace s "\"" "\\\""))

(defn kind-fn [[kind attrs]]
  (let [kind-kw (keyword "kind" (name kind))]
    (str "(defn " kind \newline
         "  \""
         (str/join \newline
                   (for [[k v] attrs]
                     (str (name k) ": " (escape v))))
         "\"" \newline
         "  ([] " kind-kw ")" \newline
         (if (:hide-code attrs)
           (str
            "  ([value] (hide-code (attach-kind-to-value value " kind-kw "))) ")
           (str
            "  ([value] (attach-kind-to-value value " kind-kw ")) ")) \newline
         "  ([value options] (" kind " [options value]))"
         ")" \newline)))

(defn kind-fns [all-kinds]
  (str/join (str \newline \newline)
            (for [[category kinds] all-kinds]
              (str ";; ## " category \newline \newline
                   (str/join \newline
                             (map kind-fn kinds))))))

(defn excludes [all-kinds]
  (let [cc (find-ns 'clojure.core)]
    (vec (for [[category kinds] all-kinds
               [kind] kinds
               :when (ns-resolve cc kind)]
           kind))))

(defn kind-ns [all-kinds]
  (str "(ns scicloj.kindly.v4.kind
  \"Kinds for visualization\"
  (:require [scicloj.kindly.v4.api :refer [attach-kind-to-value hide-code]])
  (:refer-clojure :exclude " (excludes all-kinds) "))

" (kind-fns all-kinds) \newline))

(defn known-kinds [all-kinds]
  (str "(def known-kinds
  \"A set of common visualization requests\"
  #{
" (str/join \newline
            (for [[category kinds] all-kinds]
              (str ";; " category \newline
                   (str/join \newline
                             (for [[kind] kinds
                                   :let [kind-kw (keyword "kind" (name kind))]]
                               (str "    " kind-kw))))))
       "})"
       \newline))

(defn api-ns [all-kinds]
  (str "(ns scicloj.kindly.v4.api
  \"See the kind namespace\")

(defn attach-kind-to-value
  \"Prefer using the functions in the kind namespace instead\"
  [value kind]
  (if (instance? clojure.lang.IObj value)
    (vary-meta value assoc :kindly/kind kind)
    (attach-kind-to-value [value] kind)))

(defn hide-code
  \"Annotate whether the code of this value should be hidden\"
  ([value]
    (hide-code value true))
  ([value bool]
    (if (instance? clojure.lang.IObj value)
      (vary-meta value assoc :kindly/hide-code true)
      (hide-code [value]))))

(defn consider
  \"Prefer using the functions in the kind namespace instead\"
  [value kind]
  (cond (keyword? kind) (attach-kind-to-value value kind)
        (fn? kind) (consider value (kind))))

" (known-kinds all-kinds)))

(defn -main [& args]
  (let [all-kinds (read-kinds)]
    (->> (kind-ns all-kinds)
         (spit (io/file "src" "scicloj" "kindly" "v4" "kind.cljc")))
    (->> (api-ns all-kinds)
         (spit (io/file "src" "scicloj" "kindly" "v4" "api.cljc")))))

(comment
  (-main))
