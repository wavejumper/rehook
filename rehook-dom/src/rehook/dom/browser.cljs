(ns rehook.dom.browser
  (:require
   ["react" :as react]
   [rehook.util :as util]))

(defn handle-type
  [args e ctx $]
  (cond
    (keyword? e)
    (name e)

    (util/rehook-component? e)
    (e (assoc ctx :rehook.dom/props args) $)

    (sequential? e)
    (apply react/Fragment e)

    :else e))

(defn bootstrap
  ([ctx ctx-f props-f e]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type {} e ctx (partial bootstrap ctx ctx-f props-f))]
       (react/createElement elem))))

  ([ctx ctx-f props-f e args]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type args e ctx (partial bootstrap ctx ctx-f props-f))]
       (react/createElement
        elem
        (props-f (if (contains? args :rehook/id)
                   (dissoc args :rehook/id)
                   args))))))

  ([ctx ctx-f props-f e args & children]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type args e ctx (partial bootstrap ctx ctx-f props-f))]
       (apply react/createElement
              elem
              (props-f (if (contains? args :rehook/id)
                         (dissoc args :rehook/id)
                         args))
              children)))))