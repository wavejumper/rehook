(ns rehook.dom.server
  (:require
   [rehook.util :as util]
   [rehook.dom.util :as dom.util]))

(defn handle-type
  [e ctx $ props-f args & children]
  (cond
    ;; TODO: server-side fragments???
    (#{:<> :>} e)
    (into [:div {}] e)

    (keyword? e)
    (let [[elem extra-args] (dom.util/keyword->elem e)]
      (into [(keyword elem) (props-f (dom.util/merge-arguments args extra-args))]
            children))

    (util/rehook-component? e)
    (let [props (props-f args)
          ret   (e (assoc ctx :rehook.dom/props props) $)]
      (ret props))

    (fn? e)
    (e (props-f args))))

(defn bootstrap
  ([ctx ctx-f props-f e]
   (bootstrap ctx ctx-f props-f e {}))

  ([ctx ctx-f props-f e args]
   (let [ctx (ctx-f ctx e)
         $   (partial bootstrap ctx ctx-f props-f)]
     (handle-type e ctx $ props-f args)))

  ([ctx ctx-f props-f e args child]
   (let [ctx (ctx-f ctx e)
         $   (partial bootstrap ctx ctx-f props-f)]
     (if (seq? child)
       (apply handle-type e ctx $ props-f args child)
       (handle-type e ctx $ props-f args child))))

  ([ctx ctx-f props-f e args child & children]
   (let [ctx (ctx-f ctx e)
         $   (partial bootstrap ctx ctx-f props-f)]
     (apply handle-type e ctx $ props-f args
            (if (seq? child)
              (concat child children)
              (cons child children))))))