(ns dom-test
  (:require [clojure.test :refer :all]
            [rehook.dom :as dom]
            [rehook.util :as util]
            [rehook.dom.server :as server]))

(dom/defui test-component [ctx props $]
  (dom/html $ [:div {:ctx ctx :props props}
               [:div {} "Hello world!"]]))

(deftest defui-symbol-as-component
  (let [hiccup [:div {} nil "hello world"]]
    (is (= '(:div {} "hello world")
           (dom/html list hiccup)))))

(dom/defui test-component-2-arity [ctx props]
  [:div {:ctx ctx :props props}
   [:div {} "Hello world!"]])

(dom/defui test-component-with-side-effect [{:keys [a]} _]
  (reset! a ::evaled)
  [:div {} "Hello world"])

(deftest defui--check-effects
  (let [a (atom nil)]
    ((test-component-with-side-effect {:a a} list) {})
    (is (= @a ::evaled))))

(dom/defui material-icon [_ {:keys [icon]}]
  [:i {:className "material-icons"
       :style     {:userSelect "none"}}
   icon])

(deftest button
  (is (= [:i
          {:className "material-icons", :style {:userSelect "none"}}
          [:i {:className "material-icons", :style {:userSelect "none"}} "foo"]]
         (server/bootstrap nil (constantly nil) identity material-icon {:icon [material-icon {:icon "foo"}]}))))

(deftest embedded-symbol
  (let [embedded-child [:div {} nil "foo"]]
    (is (= (dom/html list [:div {} (pr-str "foo")])
           '(:div {} "\"foo\"")))

    (is (= (dom/html list [:div {} embedded-child])
           '(:div {} (:div {} "foo"))))))

(deftest defui-macros
  (let [result ((test-component {:my :ctx}
                                list)
                {:props :my-props})
        result2 ((test-component-2-arity {:my :ctx} list)
                 {:props :my-props})]

    (is (= result
           result2
           '(:div {:ctx {:my :ctx}, :props {:props :my-props}}
             (:div {} "Hello world!"))))

    (is (util/rehook-component? test-component-2-arity))
    (is (util/rehook-component? test-component))))

(deftest anon-ui-macros
  (let [anon-component (dom/ui [ctx props $]
                         (dom/html $
                           [:div {:ctx ctx :props props}
                            [:div {} "Hello world!"]]))
        result ((anon-component {:my :ctx}
                                list)
                {:props :my-props})]

    (is (= result
           '(:div {:ctx {:my :ctx}, :props {:props :my-props}}
             (:div {} "Hello world!"))))

    (is (util/rehook-component? anon-component))))


(dom/defui nested-child-component [ctx _ $]
  (dom/html $ [:div ctx "foo"] ))

(dom/defui nested-conditional-component [ctx _ $]
  (dom/html $ [:div ctx
               (when true
                 [:div ctx
                  (when true
                    [(dom/ui [ctx _ $]
                       (dom/html $ [:div ctx "Hello world"]))])
                  (when true
                    [(dom/ui [ctx _]
                       [:div ctx "Hello world 2 arity"])])
                  "---"
                  nil
                  (when true
                    "a child")
                  (when true
                    [nested-child-component])])]))

(deftest complex-eval-logic
  (is (= [:div {:my :ctx}
          [:div {:my :ctx}
           [:div {:my :ctx}
            "Hello world"]
           [:div {:my :ctx}
            "Hello world 2 arity"]
           "---"
           "a child"
           [:div {:my :ctx}
            "foo"]]]

       (server/bootstrap {:my :ctx} (fn [x _] x) identity nested-conditional-component))))