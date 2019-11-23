(ns rehook.dom.native
  (:require
   ["react" :as react]
   ["react-native" :as rn]
   [rehook.util :as util]))

(defn handle-type
  [e ctx $]
  (cond
    (keyword? e)
    (aget rn (name e))

    (util/rehook-component? e)
    (e ctx $)

    (sequential? e)
    (apply react/Fragment e)

    :else e))

(defn bootstrap
  ([ctx ctx-f props-f e]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type e ctx (partial bootstrap ctx ctx-f props-f))]
       (react/createElement elem))))

  ([ctx ctx-f props-f e args]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type e ctx (partial bootstrap ctx ctx-f props-f))]
       (react/createElement
        elem
        (props-f (if (contains? args :rehook/id)
                   (dissoc args :rehook/id)
                   args))))))

  ([ctx ctx-f props-f e args & children]
   (let [ctx (ctx-f ctx e)]
     (when-let [elem (handle-type e ctx (partial bootstrap ctx ctx-f props-f))]
       (apply react/createElement
              elem
              (props-f (if (contains? args :rehook/id)
                         (dissoc args :rehook/id)
                         args))
              children)))))

(defn component-provider
  ([ctx component]
   (component-provider ctx identity clj->js component))
  ([ctx ctx-f props-f component]
   (constantly #(bootstrap ctx ctx-f props-f component))))