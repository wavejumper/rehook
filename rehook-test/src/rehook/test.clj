(ns rehook.test)

(defmacro io [title form]
  `(do
     (print "Called => " ~title)
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
                            e#))))))

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
               :pass  (if res# true false)}))))

(defmacro initial-render [scenes & body]
  `(let [scenes# ~scenes
         scene#  (rehook.test/mount! scenes#)]
     (try
       (binding [*scene* scene#]
         ~@body
         {:prev-scene scene# :scenes scenes#})
       (finally
         (rehook.test/unmount! scene#)))))

(defmacro next-render [prev-state & body]
  `(let [prev-state# ~prev-state
         scenes#     (:scenes prev-state#)
         prev-scene# (:prev-scene prev-state#)
         scene#      (rehook.test/mount! scenes# prev-scene#)]
     (try
       (binding [*scene* scene#]
         ~@body
         {:prev-scene scene# :scenes scenes#})
       (finally
         (rehook.test/unmount! scene#)))))

(defmacro defuitest
  [name [scenes args] & body]
  `(do
     (cljs.spec.alpha/assert* ::init-args ~args)
     (defn ~(vary-meta name assoc
                       :rehook/test? true
                       :test `(fn []
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
                                        ~scenes         scenes#]
                                    (try
                                      ~@body
                                      (assoc (deref *report*) :scenes (deref scenes#))
                                      (finally
                                        (shutdown-f# invoked-system#)))))))
       []
       (cljs.test/test-var (.-cljs$lang$var ~name)))

     (set! (.-cljs$lang$var ~name) (var ~name))
     (swap! rehook.test/registry assoc (str (var ~name)) (var ~name))))