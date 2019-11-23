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
   (if (vector? e)
     (apply eval-hiccup $ e)
     ($ e)))
  ([$ e props]
   ($ e props))
  ([$ e props & children]
   (apply $ e props (keep (fn [x]
                            (if (vector? x)
                              (apply eval-hiccup $ x)
                              x))
                          children))))

(defn compile-hiccup
  ([$ e]
   (list $ e))
  ([$ e props]
   (list $ e props))
  ([$ e props & children]
   (apply list $ e props (keep (fn [x]
                                 (cond
                                   (vector? x)
                                   (apply compile-hiccup $ x)

                                   (or (nil? x) (string? x) (number? x))
                                   x

                                   :else `(eval-hiccup ~$ ~x)))
                              children))))

#?(:clj
   (defmacro html [$ component]
     (s/assert* ::html [$ component])
     (if (vector? component)
       `~(apply compile-hiccup $ component)
       `(apply eval-hiccup ~$ ~component))))

#?(:clj
   (defmacro defui
     [name [ctx props $?] & body]
     (if $?
       (do (s/assert* ::defui [name [ctx props $?] body])
           `(def ~name
              ^{:rehook/component true
                :rehook/name      ~(str name)}
              (fn ~(gensym name) [ctx# $#]
                (let [~ctx ctx#
                      ~$? $#]
                  (fn ~(gensym name) [props#]
                    (let [~props props#]
                      ~@body))))))

       (do (s/assert* ::defui [name [ctx props] body])
           (let [$ (gensym '$)
                 effects (butlast body)
                 hiccup (last body)]
             `(def ~name
                ^{:rehook/component true
                  :rehook/name      ~(str name)}
                (fn ~(gensym name) [ctx# $#]
                  (let [~ctx ctx#
                        ~$ $#]
                    (fn ~(gensym name) [props#]
                      (let [~props props#]
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
             (let [~ctx ctx#
                   ~$? $#]
               (fn ~(gensym id) [props#]
                 (let [~props props#]
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
             (let [~ctx ctx#
                   ~$ $#]
               (fn ~(gensym id) [props#]
                 (let [~props props#]
                   ~@effects
                   (html ~$ ~hiccup)))))
           {:rehook/component true
            :rehook/name      ~(str id)})))))

