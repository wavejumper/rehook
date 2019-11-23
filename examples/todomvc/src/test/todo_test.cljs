(ns todo-test
  (:require [rehook.test :as rehook.test :refer-macros [defuitest is io initial-render next-render]]
            [todomvc.core :as todo]))

(defn test-ctx [component]
  {:system      todo/ctx
   :system-args []
   :shutdown-f  #(when-let [f (some-> % meta :stop)]
                   (f))
   :ctx-f       identity
   :props-f     identity
   :component   component})

(defuitest todo-app--end-to-end
  [scenes (test-ctx todo/todo-app)]
  (-> (initial-render scenes
        (is "Initial render should show 5 active TODO items"
          (= (rehook.test/children :items-left)
             [[:strong {} 5]
              " "
              "items"
              " left"]))

        (io "Click 'Complete all'"
          (rehook.test/invoke-prop :complete-all :onChange [{}])))

      (next-render
       (is "After clicking 'Select all', there should be 5 TODO items selected"
         (= (rehook.test/children :clear-completed) ["Clear completed " 5]))

        (io "Invoking 'Clear completed'"
          (rehook.test/invoke-prop :clear-completed :onClick [{}])))

      (next-render
       (is "After clicking 'Clear completed' there should be no TODO items left"
         (nil? (rehook.test/children :clear-completed)))

       #_(is "A demo of a failing test"
         (= true false)))))

;; defuitest isn't limited to just top-level components!
;; we can test child components as well :)
(defuitest todo-app--todo-stats
  [scenes (test-ctx (rehook.test/with-props
                     todo/todo-stats
                     {:active 1 :done 1}))]

  (-> (initial-render scenes
        (is "Initial render should show 1 items left"
          (= (rehook.test/children :items-left)
             [[:strong {} 1]
              " "
              "item"
              " left"])))))