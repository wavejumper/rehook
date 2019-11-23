(ns rehook.test
  (:require [rehook.util :as util]
            #?(:cljs [rehook.dom :refer-macros [ui]]
               :clj  [rehook.dom :refer [ui]])
            #?(:cljs [rehook.core :as rehook])
            #?(:cljs [cljs.test])
            #?(:cljs [cljs.spec.alpha :as s]
               :clj  [clojure.spec.alpha :as s])))

(s/def :init-args/system fn?)
(s/def :init-args/ctx-f fn?)
(s/def :init-args/props-f fn?)
(s/def :init-args/component any?)
(s/def :init-args/system-args (s/coll-of any?))
(s/def :init-args/shutdown-f fn?)

(s/def :defuitest/init-args
  (s/keys :req-un [:init-args/system
                   :init-args/ctx-f
                   :init-args/props-f
                   :init-args/component]
          :opt-un [:init-args/system-args
                   :init-args/shutdown-f]))

(s/def ::defuitest
  (s/cat :name symbol?
         :args (s/tuple symbol? ::init-args)
         :body (s/* any?)))

(defn- ctx-transformer [ctx elem]
  (update ctx :rehook.test/id
          #(if %
             (conj % (util/display-name elem))
             [(util/display-name elem)])))

(defn- use-state
  [scene-state local-state next-scene component-id state-id initial-value]
  (let [curr-state-id (swap! state-id inc)
        current-value (get local-state [component-id curr-state-id] initial-value)]
    (swap! scene-state assoc [component-id curr-state-id]
           {:current-value current-value
            :initial-value initial-value})
    [current-value #(when-not (= current-value %)
                      (next-scene (assoc local-state [component-id curr-state-id] %)))]))

(defn- use-effect
  [effects component-id effect-id f & [deps]]
  (let [curr-effect-id (swap! effect-id inc)]
    (swap! effects assoc [component-id curr-effect-id]
           {:deps  deps
            :f     f})))

(defn- handle-type
  [next-elements e ctx $ args raw-args children]
  (let [elem (cond
               (keyword? e)               (into [e args] children)
               ;; TODO: properly handle fragments...
               (vector? e)                (into [:*] e)
               (util/rehook-component? e) ((e ctx $) args)
               (fn? e)                    (e args))]
    (if-let [id (:rehook/id raw-args)]
      (let [elem-meta {:e        e
                       :args     raw-args
                       :evaled   elem
                       :children children}]
        (swap! next-elements assoc id elem-meta)
        elem)
      elem)))

#?(:cljs
   (defn- bootstrap
     ([next-elements next-scene scene-state effects local-state ctx ctx-f props-f e]
      (bootstrap next-elements next-scene scene-state effects local-state ctx ctx-f props-f e {}))

     ([next-elements next-scene scene-state effects local-state ctx ctx-f props-f e args & children]
      (let [ctx          (ctx-transformer (ctx-f ctx e) e)
            component-id (:rehook.test/id ctx)
            state-id     (atom 0)
            effect-id    (atom 0)]

        (with-redefs [rehook/use-effect (partial use-effect effects component-id effect-id)
                      rehook/use-state (partial use-state scene-state local-state next-scene component-id state-id)]

          (let [$ (partial bootstrap next-elements next-scene scene-state effects local-state ctx ctx-f props-f)]
            (handle-type next-elements e ctx $ (props-f args) args children)))))))

#?(:clj
   (defn- bootstrap [& args]
     (throw (ex-info "bootstrap not implemented for Clojure... yet." {:args args}))))

(defn unmount! [scene]
  (doseq [[_ umount-f] (:evaled-effects scene)]
    (umount-f)))

(defn eval-effect? [ticks prev-deps deps]
  (cond
    (= 0 ticks)                       true
    (empty? deps)                     true
    (not (identical? prev-deps deps)) true
    :else                             false))

(defn mount-scene
  [prev-scene scene]
  (let [curr-tick    (:ticks prev-scene)
        curr-effects (some-> scene :effects deref)
        prev-effects (:effects prev-scene)]
    {:render         (:render scene)
     :effects        curr-effects
     :ticks          (inc curr-tick)
     :elements       (some-> scene :elements deref)
     :evaled-effects (->> curr-effects
                          (filter (fn [[id {:keys [deps]}]]
                                    (let [prev-deps (get-in prev-effects [id :deps])]
                                      (eval-effect? curr-tick prev-deps deps))))
                          (map (fn [[id {:keys [f]}]]
                                 [id (f)]))
                          (into {}))}))

(defn init
  [ctx ctx-f props-f e]
  (let [scenes (atom {:timeline []})]
    (letfn [(next-scene [next-local-state]
              (swap! scenes update :timeline
                     (fn [timeline]
                       (let [next-effects  (atom {})
                             actions       (atom {})
                             next-elements (atom {})
                             scene-state   (atom {})
                             render        (bootstrap next-elements next-scene scene-state next-effects
                                                      next-local-state ctx ctx-f props-f e)]
                         (conj timeline {:actions  actions
                                         :render   render
                                         :index    (count timeline)
                                         :dom      #(do render)
                                         :effects  next-effects
                                         :state    scene-state
                                         :elements next-elements})))))]
      (next-scene {})
      scenes)))

(defn mount!
  ([scenes]
   (mount! scenes nil))
  ([scenes scene]
   (let [{:keys [timeline]} @scenes]
     (reduce
      (fn [prev-scene scene]
        (unmount! prev-scene)
        (mount-scene prev-scene scene))
      {:ticks 0}
      (drop (get scene :tick 0) timeline)))))

(def ^:dynamic *report* nil)
(def ^:dynamic *scene* nil)
(defonce registry (atom {}))

(defn clear-registry! []
  (reset! registry {}))

(defn children
  ([id]
   (when-not *scene*
     (throw (ex-info "rehook.test/children called outside of test" {:id id})))
   (children *scene* id))

  ([scene id]
   (-> scene :elements id :children)))

(defn get-prop
  ([id k]
   (when-not *scene*
     (throw (ex-info "rehook.test/get-prop called outside of test" {:id id :k k})))
   (get-prop *scene* id k))

  ([scene id k]
   (-> scene :elements id :args k)))

(defn invoke-prop
  ([id k args]
   (when-not *scene*
     (throw (ex-info "rehook.test/get-prop called outside of test" {:id id :k k :args args})))
   (invoke-prop *scene* id k args))

  ([scene id k args]
   (if-let [f (get-prop scene id k)]
     (apply f args)
     (throw (ex-info (str "No fn found for prop " id " on key " k)
                     {:id id :k k :args args :scene scene})))))

(defn with-props [component props]
  (ui [_ _ $]
    ($ component props)))

(defn main []
  (print "rehook.test ~~~ ♪┏(・o･)┛♪"))

#?(:clj
   (defmacro io [title form]
     `(do
        (when (nil? *report*)
          (throw (ex-info "io called outside of defuitest" {:scene *scene* :form '~form})))

        (when (nil? *scene*)
          (throw (ex-info "io called outside body of next-render or initial-render"
                          {:report *report* :form '~form})))

        (try ~form
             (swap! *report* update :tests conj
                    {:scene (-> *scene* :ticks dec)
                     :form  '~form
                     :type  :mutation
                     :title ~title})
             (catch js/Error e#
               (throw (ex-info "io failed" {:scene  *scene*
                                            :form   '~form
                                            :report (deref *report*)}
                               e#)))))))
#?(:clj
   (defmacro is [title form]
     `(do
        (when (nil? *report*)
          (throw (ex-info "assertion called outside of defuitest"
                          {:scene *scene* :form '~form})))

        (when (nil? *scene*)
          (throw (ex-info "assertion called outside body of next-render or initial-render"
                          {:report *report* :form '~form})))

        (let [res# ~form]
          (cljs.test/testing ~title
            (cljs.test/is res#))

          (swap! *report* update :tests conj
                 {:scene (-> *scene* :ticks dec)
                  :form  '~form
                  :type  :assertion
                  :title ~title
                  :pass  (if res# true false)})))))

(defn use-state-identity [val]
  [val (constantly nil)])

(defn use-effect-identity
  ([_ _]
   (constantly nil))
  ([_]
   (constantly nil)))

#?(:clj
   (defmacro initial-render [scenes & body]
     `(let [scenes# ~scenes
            scene#  (rehook.test/mount! scenes#)]
        (try
          (binding [*scene* scene#]
            (with-redefs [~'rehook.core/use-effect use-effect-identity
                          ~'rehook.core/use-state  use-state-identity]
              ~@body)
            {:prev-scene scene# :scenes scenes#})
          (finally
            (rehook.test/unmount! scene#))))))

#?(:clj
   (defmacro next-render [prev-state & body]
     `(let [prev-state# ~prev-state
            scenes#     (:scenes prev-state#)
            prev-scene# (:prev-scene prev-state#)
            scene#      (rehook.test/mount! scenes# prev-scene#)]
        (try
          (binding [*scene* scene#]
            (with-redefs [~'rehook.core/use-effect use-effect-identity
                          ~'rehook.core/use-state  use-state-identity]
              ~@body)
            {:prev-scene scene# :scenes scenes#})
          (finally
            (rehook.test/unmount! scene#))))))

#?(:clj
   (defmacro defuitest
     [name [[scenes system] args] & body]
     (let [test `(fn []
                   (binding [*report* (atom {:form  '~body
                                             :name  ~(str name)
                                             :tests []})]
                     (let [args#           ~args
                           system#         (:system args#)
                           system-args#    (or (:system-args args#) [])
                           invoked-system# (apply system# system-args#)
                           ctx-f#          (:ctx-f args#)
                           props-f#        (:props-f args#)
                           component#      (:component args#)
                           shutdown-f#     (or (:shutdown-f args#) identity)
                           scenes#         (rehook.test/init invoked-system# ctx-f# props-f# component#)
                           ~scenes         scenes#
                           ~system         invoked-system#]
                       (try
                         ~@body
                         (assoc (deref *report*) :scenes (deref scenes#))
                         (finally
                           (shutdown-f# invoked-system#))))))]
       `(do
          #_(cljs.spec.alpha/assert* :defuitest/init-args ~args)
          (defn ~(vary-meta name assoc
                            :rehook/test? true
                            :test test)
            []
            (cljs.test/test-var (.-cljs$lang$var ~name)))

          (set! (.-cljs$lang$var ~name) (var ~name))
          (swap! rehook.test/registry assoc
                 ;; in :advanced optimisations, :test doesn't seem to be affixed
                 ;; after calling vary-meta.
                 ;; this macro is already a lil hairy, yolo, etc
                 (str (var ~name)) (assoc (meta (var ~name)) :test ~test))))))