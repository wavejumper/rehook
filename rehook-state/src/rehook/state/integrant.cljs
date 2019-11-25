(ns rehook.state.integrant
  (:require [integrant.core :as ig]
            [rehook.state :as state]))

(defmethod ig/init-key :rehook/db [_ initial-value]
  (atom initial-value))

(defmethod ig/init-key :rehook/events [_ opts]
  (state/init-state opts))

(defn subscribe
  ([sys sub]
   (subscribe sys :rehook/events sub))

  ([sys k sub]
   (let [f (get-in sys [k :subscribe])]
     (f sub))))

(defn dispatch
  ([sys event]
   (dispatch sys :rehook/events event))

  ([sys k event]
   (let [f (get-in sys [k :dispatch])]
     (f event))))

(defn dispatch-fx
  ([sys event]
   (dispatch-fx sys :rehook/events event))

  ([sys k event]
   (let [f (get-in sys [k :dispatch-fx])]
     (f event))))

(defn ig->ctx
  ([sys]
   (ig->ctx sys :rehook/events))
  ([sys k]
   {:dispatch    (partial dispatch sys k)
    :subscribe   (partial subscribe sys k)
    :dispatch-fx (partial dispatch-fx k)}))