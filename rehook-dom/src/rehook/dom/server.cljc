(ns rehook.dom.server
  (:require [rehook.util :as util]))

(defn handle-type
  [e ctx $ args children]
  (cond
    (keyword? e)
    (into [e args] children)

    ;; TODO: server-side fragments???
    (sequential? e)
    (into [:div {}] e)

    (util/rehook-component? e)
    (let [ret (e (assoc ctx :rehook.dom/props args) $)]
      (ret args))

    (fn? e)
    (e args)))

(defn bootstrap
  ([ctx ctx-f props-f e]
   (bootstrap ctx ctx-f props-f e {}))

  ([ctx ctx-f props-f e args & children]
   (let [ctx (ctx-f ctx e)
         $   (partial bootstrap ctx ctx-f props-f)]
     (handle-type e ctx $ (props-f args) children))))