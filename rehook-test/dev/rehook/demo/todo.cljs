(ns rehook.demo.todo
  (:require [rehook.core :as rehook]
            [rehook.dom :refer-macros [defui]]
            [rehook.dom.browser :as dom]))

(defn add-todo [counter todos text]
  (let [id (swap! counter inc)]
    (swap! todos assoc id {:id id :title text :done false})))

(defn toggle [todos id] (swap! todos update-in [id :done] not))
(defn save [todos id title] (swap! todos assoc-in [id :title] title))
(defn delete [todos id] (swap! todos dissoc id))

(defn mmap [m f a] (->> m (f a) (into (empty m))))
(defn complete-all [todos v] (swap! todos mmap map #(assoc-in % [1 :done] v)))
(defn clear-done [todos]
  (swap! todos mmap remove #(get-in % [1 :done])))

(defn init [counter todos]
  (let [add-todo (partial add-todo counter todos)]
    (add-todo "Rename Cloact to Reagent")
    (add-todo "Add undo demo")
    (add-todo "Make all rendering async")
    (add-todo "Allow any arguments to component functions")
    (complete-all todos true)))

(defn new-system []
  (let [todos       (atom (sorted-map))
        counter     (atom 0)
        todo-filter (atom {:filter :all})]
    {:todos       todos
     :counter     counter
     :todo-filter todo-filter
     :events      {:add-todo     (partial add-todo counter todos)
                   :complete-all (partial complete-all todos)
                   :clear-done   (partial clear-done todos)
                   :toggle       (partial toggle todos)
                   :save         (partial save todos)
                   :delete       (partial delete todos)}
     :init #(init counter todos)}))

(defui todo-input [_ props $]
  (let [{:keys [title onSave onStop id class placeholder]} (js->clj props :keywordize-keys true)
        [val setter] (rehook/use-state (or title ""))
        stop #(do (setter "")
                  (when onStop (onStop)))
        save #(let [v (-> val str clojure.string/trim)]
                (if-not (empty? v)
                  (onSave v)
                  (stop)))]
    ($ :input {:type        "text"
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
                               nil)})))

(defui todo-stats [{:keys [todo-filter events]} props $]
  (let [{:keys [active done]} (js->clj props :keywordize-keys true)
        clear (:clear-done events)
        [filt set-filt] (rehook/use-atom-path todo-filter [:filter])]
    ($ :div {}
       ($ :span {:id "todo-count"
                 :rehook/id :items-left}
          ($ :strong {} active) " " (case active 1 "item" "items") " left")
       ($ :ul {:id "filters"}
          ($ :li {}
             ($ :a {:className   (if (= :all filt) "selected")
                    :onClick #(set-filt :all)}
                "All"))
          ($ :li {}
             ($ :a {:className   (if (= :done filt) "selected")
                    :onClick #(set-filt :active)}
                "Active"))
          ($ :li {}
             ($ :a {:className   (if (= :done filt) "selected")
                    :onClick #(set-filt :done)}
                "Completed")))
       (when (pos? done)
         ($ :button {:id        "clear-completed"
                     :rehook/id :clear-completed
                     :onClick   clear}
            "Clear completed " done)))))

(defui todo-item [ctx props $]
  (let [{:keys [id done title]} (js->clj props :keywordize-keys true)
        toggle (-> ctx :events :toggle)
        delete (-> ctx :events :delete)
        save   (-> ctx :events :save)
        [editing setter] (rehook/use-state false)]

    ($ :li {:className (str (if done "completed ")
                            (if editing "editing"))}
       ($ :div {:className "view"}
          ($ :input {:className "toggle"
                     :type "checkbox"
                     :checked done
                     :onChange #(toggle id)})
          ($ :label {:onDoubleClick #(setter true)}
             title)
          ($ :button {:className "destroy"
                      :onClick #(delete id)}
             "Delete")
          (when editing
            ($ todo-input {:className "edit"
                           :title     title
                           :onSave    #(save id %)
                           :onStop    #(setter false)}))))))

(defui todo-app
  [{:keys [todo-filter todos] :as ctx} _ $]
  (let [[filt _] (rehook/use-atom-path todo-filter [:filter])
        [todos _] (rehook/use-atom todos)
        complete-all (-> ctx :events :complete-all)
        add-todo (-> ctx :events :add-todo)
        items  (vals todos)
        done   (->> items (filter :done) count)
        active (- (count items) done)]

    ($ :div {}
       ($ :section {:id "todoapp"}
          ($ :header {:id "header"}
             ($ :h1 {} "todos (rehook)")
             ($ todo-input {:id "new-todo"
                            :placeholder "What needs to be done?"
                            :onSave add-todo}))
          (when (not-empty items)
            ($ :div {}
               ($ :section {:id "main"}
                  ($ :input {:id       "toggle-all"
                             :type     "checkbox"
                             :checked  (zero? active)
                             :onChange #(complete-all (pos? active))})
                  ($ :label {:htmlFor "toggle-all"}
                     "Mark all as complete")
                  (let [filtered-items (filter (case filt
                                                 :active (complement :done)
                                                 :done :done
                                                 :all identity)
                                               items)]
                    (apply $ :ul {:id "todo-list"}
                           (map #($ todo-item (clj->js (assoc % :key (:id %))))
                                filtered-items))))
               ($ :footer {:id "footer"}
                  ($ todo-stats {:active active
                                 :done done}))))

          ($ :footer {:id "footer"}
             ($ :p {} "Double-click to edit a todo"))))))

(defn system []
  (let [sys (new-system)]
    ((:init sys))
    sys))

(defn main []
  (dom/bootstrap system identity clj->js todo-app))