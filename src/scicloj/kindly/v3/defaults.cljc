(ns scicloj.kindly.v3.defaults
  (:require [scicloj.kindly.v3.api :as kindly]
            [scicloj.kindly.v3.impl :as impl]
            [scicloj.kindly.v3.kindness :as kindness]))

(defn form->kind [form]
  (when-let [m (some-> form meta)]
    (or (some->> m
                 :tag
                 resolve
                 deref
                 namespace
                 (= "kind"))
        (some->> m
                 keys
                 (filter #(-> %
                              namespace
                              (= "kind")))
                 first))))

(defn value->kind [value]
  (or (-> value
          meta
          :kindly/kind)
      (-> value
          kindness/kind)))

(defn kind
  ([value form]
   (or (-> form
           form->kind)
       (-> value
           value->kind))))

(defn advice [{:as context
               :keys [value form]}]
  (if (:kind context)
    context
    (let [k (kind value form)]
      (assoc context
             :kind k))))

(defn setup! []
  (kindly/set-only-advice! #'advice))