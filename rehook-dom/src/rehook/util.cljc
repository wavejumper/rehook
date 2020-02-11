(ns rehook.util)

(defn react-props [props]
  (some-> props meta :react/props))

(defn rehook-component? [e]
  (-> e meta :rehook/component true?))

(defn display-name [e]
  (cond
    (keyword? e) (name e)
    (rehook-component? e) (-> e meta :rehook/name)
    :else (aget e "displayName")))