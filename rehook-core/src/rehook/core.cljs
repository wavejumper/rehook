(ns rehook.core
  (:require ["react" :as react]))

(defn use-state
  [initial-value]
  (react/useState initial-value))

(defn use-effect
  ([f]
   (react/useEffect f))
  ([f deps]
   (react/useEffect f (to-array deps))))

(defn use-atom-fn
  [a getter-fn setter-fn]
  (let [[val set-val] (use-state (getter-fn @a))]

    (use-effect
     (fn []
       (let [id (str (random-uuid))]
         (add-watch a id (fn [_ _ prev-state next-state]
                           (let [prev-value (getter-fn prev-state)
                                 next-value (getter-fn next-state)]
                             (when-not (= prev-value next-value)
                               (set-val next-value)))))
         #(remove-watch a id)))
     [])

    [val #(swap! a setter-fn %)]))

(defn use-atom
  "(use-atom my-atom)"
  [a]
  (use-atom-fn a identity (fn [_ v] v)))

(defn use-atom-path
  "(use-atom my-atom [:path :to :data])"
  [a path]
  (use-atom-fn a #(get-in % path) #(assoc-in %1 path %2)))
