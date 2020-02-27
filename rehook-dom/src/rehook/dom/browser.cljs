(ns rehook.dom.browser
  (:require
   ["react" :as react]
   [rehook.dom.util :as dom.util]
   [rehook.util :as util]))

(defn handle-type
  [args e ctx $]
  (cond
    (= :> e)
    [react/Fragment args]

    (keyword? e)
    (let [[elem extra-args] (dom.util/keyword->elem e)]
      [elem (merge args extra-args)])

    (util/rehook-component? e)
    (let [rehook-component (e (assoc ctx :rehook.dom/props args) $)]
      (when-not (aget rehook-component "displayName")
        (aset rehook-component "displayName" (util/display-name e)))
      [rehook-component args])

    :else [e args]))

(defn bootstrap
  ([ctx ctx-f props-f e]
   (let [ctx (ctx-f ctx e)
         [elem args] (handle-type {} e ctx (partial bootstrap ctx ctx-f props-f))]
     (when elem
       (react/createElement
        elem
        (props-f (if (contains? args :rehook/id)
                   (dissoc args :rehook/id)
                   args))))))

  ([ctx ctx-f props-f e args]
   (let [ctx (ctx-f ctx e)]
     (let [[elem args] (handle-type args e ctx (partial bootstrap ctx ctx-f props-f))]
       (when elem
         (react/createElement
          elem
          (props-f (if (contains? args :rehook/id)
                     (dissoc args :rehook/id)
                     args)))))))

  ([ctx ctx-f props-f e args child]
   (let [ctx (ctx-f ctx e)
         [elem args] (handle-type args e ctx (partial bootstrap ctx ctx-f props-f))]
     (when elem
       (if (seq? child)
         (apply react/createElement
                elem
                (props-f (if (contains? args :rehook/id)
                           (dissoc args :rehook/id)
                           args))
                child)

         (react/createElement
          elem
          (props-f (if (contains? args :rehook/id)
                     (dissoc args :rehook/id)
                     args))
          child)))))

  ([ctx ctx-f props-f e args child & children]
   (let [ctx (ctx-f ctx e)
         [elem args] (handle-type args e ctx (partial bootstrap ctx ctx-f props-f))]
     (when elem
       (apply react/createElement
              elem
              (props-f (if (contains? args :rehook/id)
                         (dissoc args :rehook/id)
                         args))
              (cons child children))))))