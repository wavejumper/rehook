(ns rehook.util
  #?(:cljs (:require ["react" :as react])))

(defn react-props [props]
  (some-> props meta :react/props))

(defn children [props]
  (some-> props react-props (aget "children")))

#?(:cljs
   (defn child-seq [props]
     (some-> props children react/Children.toArray)))

(defn rehook-component? [e]
  (-> e meta :rehook/component true?))

(defn display-name [e]
  (cond
    (keyword? e) (name e)
    (rehook-component? e) (-> e meta :rehook/name)
    :else (aget e "displayName")))