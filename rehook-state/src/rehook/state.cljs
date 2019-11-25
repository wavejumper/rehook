(ns rehook.state
  (:require [rehook.core :as rehook]))

(defn init-state
  [{:keys [db ctx subscriptions events effects]}]
  {:subscriptions subscriptions
   :events events
   :effects effects
   :subscribe (fn [[id & args]]
                (if-let [subscription (get subscriptions id)]
                  (first (rehook/use-atom-fn db #(subscription % args) (constantly nil)))
                  (js/console.warn (str "No subscription found for id " id))))
   :dispatch-fx (fn [[id & args]]
                  (if-let [effect (get effects id)]
                    (effect ctx db args)
                    (js/console.warn (str "No effect found for id " id))))
   :dispatch (fn [[id & args]]
               (if-let [handler (get events id)]
                 (swap! db #(handler % ctx args))
                 (js/console.warn (str "No event handler found for id " id))))})