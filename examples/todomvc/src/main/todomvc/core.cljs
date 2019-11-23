(ns todomvc.core
  (:require [rehook.core :as rehook]
            [rehook.dom :refer-macros [defui]]
            [rehook.dom.browser :as dom]
            [integrant.core :as ig]
            ["react-dom" :as react-dom]))

(defmethod ig/init-key :app/db [_ initial-state]
  (atom initial-state))

;; a very naive re-frame impl containing just subscriptions and events
(defmethod ig/init-key :rehook/reframe
  [_ {:keys [db subscriptions events]}]
  {:subscriptions subscriptions
   :events events
   :subscribe (fn [[id & args]]
                (if-let [subscription (get subscriptions id)]
                  (first (rehook/use-atom-fn db #(subscription % args) (constantly nil)))
                  (js/console.warn (str "No subscription found for id " id))))
   :dispatch (fn [[id & args]]
               (if-let [handler (get events id)]
                 (swap! db #(handler % args))
                 (js/console.warn (str "No event handler found for id " id))))})

(def initial-items
  {0 {:id 0 :title "Rename Cloact to Reagent" :done false}
   1 {:id 1 :title "Add undo demo" :done false}
   2 {:id 2 :title "Make all rendering async" :done false}
   3 {:id 3 :title "Allow any arguments to component functions" :done false}
   4 {:id 4 :title "Update demo items to not use Reagent's example :p" :done false}})

(defn add-todo [db [title]]
  (let [id (inc (:counter db))]
    (-> db
        (assoc :counter id)
        (update :todos assoc id {:id id :title title :done false}))))

(defn complete-all [db _]
  (update db :todos
          (fn [todos]
            (into {} (map (fn [[id todo]]
                            [id (assoc todo :done true)]))
                  todos))))

(defn clear-done [db _]
  (update db :todos #(into {} (remove (fn [[_ todo]] (:done todo))) %)))

(defn save [db [id title]]
  (assoc-in db [:todos id :title] title))

(defn toggle [db [id]]
  (update-in db [:todos id :done] not))

(defn delete [db [id]]
  (update db :todos dissoc id))

(defn set-filter [db [next-filter]]
  (assoc-in db :filter next-filter))

(def events
  {:add-todo     add-todo
   :complete-all complete-all
   :clear-done   clear-done
   :save         save
   :toggle       toggle
   :delete       delete
   :set-filter   set-filter})

(defn todo-filter [db]
  (:filter db))

(defn todos [db]
  (:todos db))

(def subscriptions
  {:filter todo-filter
   :todos  todos})

(defn config []
  {:app/db {:todos   initial-items
            :counter (count initial-items)
            :filter  :all}
   :rehook/reframe {:db (ig/ref :app/db)
                    :events events
                    :subscriptions subscriptions}})

(defui todo-input [_ props]
  (let [{:keys [title onSave onStop id class placeholder]} (js->clj props :keywordize-keys true)
        [val setter] (rehook/use-state (or title ""))
        stop #(do (setter "")
                  (when onStop (onStop)))
        save #(let [v (-> val str clojure.string/trim)]
                (if-not (empty? v)
                  (onSave v)
                  (stop)))]
    [:input {:type        "text"
             :rehook/id   :todo-input
             :value       val
             :id          id
             :className   class
             :placeholder placeholder
             :onBlur      save
             :onChange    #(setter (-> % .-target .-value))
             :onKeyDown   #(case (.-which %)
                             13 (save)
                             27 (stop)
                             nil)}]))

(defui todo-stats [{:keys [dispatch subscribe]} props]
  (let [{:keys [active done]} (js->clj props :keywordize-keys true)
        filt  (subscribe [:filter])]
    [:div {}
     [:span {:id        "todo-count"
             :rehook/id :items-left}
      [:strong {} active] " " (case active 1 "item" "items") " left"]
     [:ul {:id "filters"}
      [:li {}
       [:a {:className (if (= :all filt) "selected")
            :onClick   #(dispatch [:set-filter :all])}
        "All"]]

      [:li {}
       [:a {:className (if (= :done filt) "selected")
            :onClick   #(dispatch [:set-filter :active])}
        "Active"]]

      [:li {}
       [:a {:className (if (= :done filt) "selected")
            :onClick   #(dispatch [:set-filter :done])}
        "Completed"]]

      (when (pos? done)
        [:button {:id        "clear-completed"
                  :rehook/id :clear-completed
                  :onClick   #(dispatch [:clear-done])}
         "Clear completed " done])]]))

(defui todo-item [{:keys [dispatch]} props]
  (let [{:keys [id done title]} (js->clj props :keywordize-keys true)
        [editing setter] (rehook/use-state false)]
    [:li {:className (str (if done "completed ")
                          (if editing "editing"))}
     [:div {:className "view"}
      [:input {:className "toggle"
               :type      "checkbox"
               :checked   done
               :onChange  #(dispatch [:toggle id])}]
      [:label {:onDoubleClick #(setter true)}
       title]
      [:button {:className "destroy"
                :onClick   #(dispatch [:delete id])}
       "Delete"]
      (when editing
        [todo-input {:className "edit"
                     :title     title
                     :onSave    #(dispatch [:save id %])
                     :onStop    #(setter false)}])]]))

(defui todo-app
  [{:keys [dispatch subscribe]} _]
  (let [filt  (subscribe [:filter])
        todos (subscribe [:todos])
        items  (vals todos)
        done   (->> items (filter :done) count)
        active (- (count items) done)]

    [:div {}
     [:section {:id "todoapp"}
      [:header {:id "header"}
       [:h1 {} "todos (rehook)"]
       [todo-input {:id          "new-todo"
                    :placeholder "What needs to be done?"
                    :onSave      #(dispatch [:add-todo %])}]]
      (when (not-empty items)
        [:div {}
         [:section {:id "main"}
          [:input {:id       "toggle-all"
                   :type     "checkbox"
                   :checked  (zero? active)
                   :rehook/id :complete-all
                   :onChange #(dispatch [:complete-all (pos? active)])}]
          [:label {:htmlFor "toggle-all"}
           "Mark all as complete"]
          (let [filtered-items (filter (case filt
                                         :active (complement :done)
                                         :done :done
                                         :all identity)
                                       items)]
            (into [:ul {:id "todo-list"}]
                  (map (fn [item] [todo-item (assoc item :key (:id item))])
                        filtered-items)))
          [:footer {:id "footer"}
           [todo-stats {:active active
                        :done   done}]]]])

      [:footer {:id "footer"}
       [:p {} "Double-click to edit a todo"]]]]))

(defn subscribe [sys sub]
  (let [f (get-in sys [:rehook/reframe :subscribe])]
    (f sub)))

(defn dispatch [sys event]
  (let [f (get-in sys [:rehook/reframe :dispatch])]
    (f event)))

;; Instead of passing the entire application context to our react components
;; we can pass our re-frame like interface instead!
;;
;; If we update our event or subscription maps, we can simply call (dev/reload)
(defn ctx []
  (let [system (ig/init (config))]
    ^{:stop #(ig/halt! system)}
    {:dispatch  (partial dispatch system)
     :subscribe (partial subscribe system)}))

(defn main []
  (let [elem (dom/bootstrap (ctx) (fn [ctx _] ctx) clj->js todo-app)]
    (react-dom/render elem (js/document.getElementById "app"))))