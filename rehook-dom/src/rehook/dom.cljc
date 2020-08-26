(ns rehook.dom
  (:require [clojure.spec.alpha :as s]))

(s/def ::arg
  (s/or :symbol? symbol?
        :map?    map?))

(s/def ::args
  (s/or
   :two-arity   (s/tuple ::arg ::arg)
   :three-arity (s/tuple ::arg ::arg symbol?)))

(s/def ::defui
  (s/cat :name symbol?
         :args ::args
         :body (s/* any?)))

(s/def ::ui
  (s/cat :args ::args
         :body (s/* any?)))

(s/def ::html
  (s/cat :render-fn symbol?
         :component any?))

(defn eval-hiccup
  ([$ e]
   (cond
     (seq? e)
     (map #(apply $ %) e)

     (vector? e)
     (apply eval-hiccup $ e)

     (or (nil? e) (string? e) (number? e))
     e

     :else ($ e)))

  ([$ e props]
   ($ e props))

  ([$ e props & children]
   (apply $ e props
          (->> children
               (mapcat (fn [child]
                         (cond
                           (and (vector? child) (seq child))
                           [(apply eval-hiccup $ child)]

                           (seq? child)
                           (map #(eval-hiccup $ %) child)

                           :else
                           [(eval-hiccup $ child)])))
               (filter identity)))))

(defn compile-hiccup
  ([$ e]
   (list $ e))

  ([$ e props]
   (list $ e props))

  ([$ e props child]
   (list $ e props
         (cond
           (vector? child)
           (apply compile-hiccup $ child)

           (or (nil? child) (string? child) (number? child))
           child

           :else
           `(eval-hiccup ~$ ~child))))

  ([$ e props child & children]
   (let [children (cons child children)]
     (apply list $ e props (keep (fn [e]
                                   (cond
                                     (vector? e)
                                     (apply compile-hiccup $ e)

                                     (or (nil? e) (string? e) (number? e))
                                     e

                                     :else
                                     `(eval-hiccup ~$ ~e)))
                                 children)))))

#?(:clj
   (defmacro html [$ component]
     (s/assert* ::html [$ component])
     (if (vector? component)
       `~(apply compile-hiccup $ component)
       `(apply eval-hiccup ~$ ~component))))

(defn rehook-meta
  [m props _]
  (assoc m :react/props props))

(defn as-element
  "Turns a vector of Hiccup syntax into a React element."
  [ctx form]
  (if-let [$ (get ctx :rehook.dom/bootstrap)]
    (eval-hiccup $ form)
    (if ctx
      (throw (ex-info "ctx argument to as-element null. Have you passed in the ctx map?" {}))
      (throw (ex-info "as-element requires :rehook.dom/bootstrap key in ctx map. Are you you using defui?"
                      {:keys (keys ctx) :ctx ctx})))))

#?(:clj
   (defmacro defui
     [name [ctx props $?] & body]
     (if $?
       (do (s/assert* ::defui [name [ctx props $?] body])
           `(def ~name
              ^{:rehook/component true
                :rehook/name      ~(str name)}
              (fn ~name [ctx# $#]
                (let [~ctx (dissoc ctx# :rehook.dom/props)
                      ~$? $#]
                  (fn ~(gensym name) [props#]
                    (let [~props (vary-meta (get ctx# :rehook.dom/props {}) rehook-meta props# ctx#)]
                      ~@body))))))

       (do (s/assert* ::defui [name [ctx props] body])
           (let [$ (gensym '$)
                 effects (butlast body)
                 hiccup (last body)]
             `(def ~name
                ^{:rehook/component true
                  :rehook/name      ~(str name)}
                (fn ~name [ctx# $#]
                  (let [~ctx (dissoc ctx# :rehook.dom/props)
                        ~$ $#]
                    (fn ~(gensym name) [props#]
                      (let [~props (vary-meta (get ctx# :rehook.dom/props {}) rehook-meta props# ctx#)]
                        ~@effects
                        (html ~$ ~hiccup)))))))))))

#?(:clj
   (defmacro ui
     [[ctx props $?] & body]
     (if $?
       (let [id (gensym "ui")]
         (s/assert* ::ui [[ctx props $?] body])
         `(with-meta
           (fn ~id [ctx# $#]
             (let [~ctx (dissoc ctx# :rehook.dom/props)
                   ~$? $#]
               (fn ~(gensym id) [props#]
                 (let [~props (vary-meta (get ctx# :rehook.dom/props {}) rehook-meta props# ctx#)]
                   ~@body))))
           {:rehook/component true
            :rehook/name      ~(str id)}))

       (let [id      (gensym "ui")
             $       (gensym '$)
             effects (butlast body)
             hiccup  (last body)]
         (s/assert* ::ui [[ctx props] body])
         `(with-meta
           (fn ~id [ctx# $#]
             (let [~ctx (dissoc ctx# :rehook.dom/props)
                   ~$ $#]
               (fn ~(gensym id) [props#]
                 (let [~props (vary-meta (get ctx# :rehook.dom/props {}) rehook-meta props# ctx#)]
                   ~@effects
                   (html ~$ ~hiccup)))))
           {:rehook/component true
            :rehook/name      ~(str id)})))))