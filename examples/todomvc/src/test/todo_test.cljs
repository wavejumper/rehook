(ns todo-test
  (:require [rehook.test :as rehook.test :refer-macros [defuitest is io initial-render next-render]]
            [rehook.demo.todo :as todo]))

(defn test-ctx [component]
  {:system      todo/system
   :system-args []
   :shutdown-f  identity
   :ctx-f       identity
   :props-f     identity
   :component   component})

(defuitest todo-app--clear-completed
  [scenes (test-ctx todo/todo-app)]
  (-> (initial-render scenes
        (is "Initial render should show 4 TODO items"
          (= (rehook.test/children :clear-completed) ["Clear completed " 4]))

        (io "Click 'Clear completed'"
          (rehook.test/invoke-prop :clear-completed :onClick [{}])))

      (next-render
       (is "After clicking 'Clear Completed', there should be no TODO items"
         (nil? (rehook.test/children :clear-completed)))

        (io "Invoking todo-input onChange"
          (rehook.test/invoke-prop :todo-input :onChange [(clj->js {:target {:value "foo"}})])))

      (next-render
       (is "After inputting text, value should appear in input"
         (= "foo" (rehook.test/get-prop :todo-input :value)))

       (io "Pressing enter button on todo-input"
         (rehook.test/invoke-prop :todo-input :onKeyDown [(clj->js {:which 13})])))

      (next-render
       (is "After pressing enter button there should be one item to do"
         (= 1 1))

       (is "After pressing enter button, input's value should be cleared."
         (empty? (rehook.test/get-prop :todo-input :value))))))

;; defuitest isn't limited to just top-level components!
;; we can test child components as well :)
(defuitest todo-app--todo-stats
  [scenes (test-ctx (rehook.test/with-props
                     todo/todo-stats
                     {:active 1 :done 1}))]

  (-> (initial-render scenes
        (is "Initial render should show 1 item left"
          (= (rehook.test/children :items-left)
             [[:strong {} 1]
              " "
              "item"
              " left"])))))