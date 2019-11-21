# rehook

Clojurescript React library enabling data-driven architecture

## About

rehook is built from small, modular blocks - each with an explicit notion of time, and a data-first design.

As rehook is modular, each layer builds upon the last. Each layer adds a new idea: testing, syntax, devtools, patterns.

If you need a primer on what React hooks, the [API docs](https://reactjs.org/docs/hooks-reference.html) are a good start.

The core library tries to do two things:

* marry react hooks with Clojure atoms
* avoid singleton state

It is my hope that rehook's core API could be used to build general and domain-specific abstractions on top: eg re-frame impls, om-next style querying etc.

Its modular design, and guiding philosophy have already enabled some rich tooling already like rehook-test. 

## Usage

## rehook.core

`rehook.core` exposes 5 useful functions for state and effects:

- `use-state` convenient wrapper over `react/useState`
- `use-effect` convenient wrapper over `react/useEffect`
- `use-atom` use a Clojure atom (eg, for global app state) within a component
- `use-atom-path` like `use-atom`, except for a path into a atom (eg, `get-in`)
- `use-atom-fn` provide custom getter/setter fns to build your own abstractions

## rehook.dom 

`rehook.dom` provides hiccup syntax.

`rehook.dom` provides a baggage free way to pass down application context (eg, [integrant](https://github.com/weavejester/integrant) or [component](https://github.com/stuartsierra/component)) as you will see below.

## Usage

```clojure 
(ns demo 
  (:require 
    [rehook.core :as rehook]
    [rehook.dom :refer-macros [defui]]
    [react.dom.browser :as dom.browser]
    ["react-dom" :as react-dom]))

(defn system [] ;; <-- system map (this could be integrant, component, etc)
  {:state (atom {:missiles-fired? false})})

(defui my-component 
  [{:keys [state]} ;; <-- context map from bootstrap fn
   props] ;; <-- any props passed from parent component
  (let [[curr-state _]                       (rehook/use-atom state) ;; <-- capture the current value of the atom
        [debug set-debug]                    (rehook/use-state false) ;; <-- local state
        [missiles-fired? set-missiles-fired] (rehook/use-atom-path state [:missiles-fired?])] ;; <-- capture current value of path in atom

    (rehook/use-effect
      (fn []
        (js/console.log (str "Debug set to " debug)) ;; <-- the side-effect invoked after the component mounts and debug's value changes
        (constantly nil)) ;; <-- the side-effect to be invoked when the component unmounts
      [debug])

    [:section {}
      [:div {}
        (if debug 
          [:span {:onClick #(set-debug false)} "Hide debug"]
          [:span {:onClick #(set-debug true)} "Show debug"])
        (when debug
          (pr-str curr-state))]

      (if missiles-fired?
        [:div {} "Missiles have been fired!"]
        [:div {:onClick #(set-missiles-fired true)} "Fire missiles"])]))

;; How to render a component to the DOM
(react-dom/render 
  (dom.browser/bootstrap 
    (system) ;; <-- context map
    identity ;; <-- context transformer
    clj->js ;; <-- props transformer
    my-component) ;; <-- root component
  (js/document.getElementById "myapp"))
```

### Hooks gotchas

* When using `use-effect`, make sure the values of `deps` pass Javascript's notion of equality! Solution: use simple values instead of complex maps.
* Enforced via convention, React hooks and effects need to be defined at the top-level of your component (and not bound conditionally) 

## defui 

`rehook.dom/defui` is a macro used to define `rehook` components. This macro is only syntactic sugar, as all `rehook` components are cljs fns.

`defui` takes in two arguments:

* `context`: immutable, application context
* `props`: any props passed to the component. This will be an untouched JS object from React.

It must return valid hiccup.

```clojure
(ns demo 
  (:require [rehook.dom :refer-macros [defui]]))

(defui my-component [{:keys [dispatch]} _] 
  [:text {:onClick #(dispatch :fire-missles)} "Fire missles!"])
```

The anonymous counterpart is `rehook.dom/ui`

### fragments

Simply return a collection of hiccup:

```clojure
(defui fragmented-ui [_ _]
  [[:div {} "Div 1"] [:div {} "Div 2"]])
```

### rehook components

Reference the component directly:

```clojure
(defui child [_ _] 
  [:div {} "I am the child"])
  
(defui parent [_ _]
  [child])
```

### reactjs components

Same as rehook components. Reference the component directly:

```clojure
(require '["react-select" :as ReactSelect])

(defui select [_ props]
  [ReactSelect props])
```

### reagent components 

```clojure
(require '[reagent.core :as r])

(defn my-reagent-component []
  [:div {} "I am a reagent component, I guess..."])

(defui my-rehook-component [_ _]
  [(r/reactify-component my-reagent-component)])
```

### hiccup-free

You can opt-out of the `html` macro by passing a third argument (the render fn) to `defui`:

```clojure
(defui no-html-macro [_ _ $]
  ($ :div {} "rehook-dom without hiccup!"))
```

Because the `$` render fn is passed into every rehook component you can overload it -- or better yet create your own abstract macros!

## Props

A props transformation fn is passed to the initial `bootstrap` fn. The return value of this fn must be a JS object.

A good default to use is `cljs.core/clj->js`. 

If you want to maintain Clojure idioms, a library like [camel-snake-kebab](https://github.com/clj-commons/camel-snake-kebab) could be used to convert keys in your props (eg, `on-press` to `onPress`)

## Initializing

## react-dom

You can call `react-dom/render` directly, and `bootstrap` your component:

```clojure 
(ns example.core 
  (:require 
    [example.components :refer [app]]
    [rehook.dom.browser :as dom]
    ["react-dom" :as react-dom]))

(defn system []
  {:dispatch (fn [& _] (js/console.log "TODO: implement dispatch fn..."))})

(defn main []
  (react-dom/render 
    (dom/bootstrap (system) identity clj->js app)
    (js/document.getElementById "app")))
```

## react-native

You can use the `rehook.dom.native/component-provider` fn if you directly call [AppRegistry](https://facebook.github.io/react-native/docs/appregistry)

```clojure 
(ns example.core
  (:require 
    [rehook.dom :refer-macros [defui]]
    [rehook.dom.native :as dom]
    ["react-native" :refer [AppRegistry]]))

(defui app [{:keys [dispatch]} _]
  [:Text {:onPress #(dispatch :fire-missles)} "Fire missles!"])

(defn system []
  {:dispatch (fn [& _] (js/console.log "TODO: implement dispatch fn..."))})

(defn main []
  (.registerComponent AppRegistry "my-app" (dom/component-provider (system) app))
```

Alternatively, if you don't have access to the `AppRegistry`, you can use the `rehook.dom.native/boostrap` fn instead - which will return a valid React element

## Context transformer

The context transformer can be incredibly useful for instrumentation, or for adding additional abstractions on top of the library (eg implementing your own data flow engine ala [domino](https://domino-clj.github.io/))

For example:

```clojure 
(require '[rehook.util :as util])

(defn ctx-transformer [ctx component]  
  (update ctx :log-ctx #(conj (or % []) (util/display-name component))))

(dom/component-provider (system) ctx-transformer clj->js app)
```

In this example, each component will have the hierarchy of its parents in the DOM tree under the key `:log-ctx`. 

This can be incredibly useful context to pass to your logging/metrics library!

## Testing

rehook allows you to test your entire application - from data layer to view. 

How? Because `rehook` promotes building applications with no singleton global state. Therefore, you can treat your components as 'pure functions', as all inputs to the component are passed in as arguments.

rehook-test supports:

* server, react-dom and react-native
* cljs.test + nodejs target for headless/CI
* browser for devcards-like interactive development
* whatever else you can think of. it's just a function call really.
 
### rehook-test

![image](https://i.imgur.com/zQEwOjX.png)

A demo of rehook-test in action.

### Time-travel driven development

Writing tests for rehook is not dissimilar to how you might test with datomic or kafka's TopologyTestDriver, with a bit of devcards in the mix.

Each state change produces a snapshot in time that rehook captures as a 'scene'.

Like kafka's ToplogyTestDriver, the tests run in a simulated library runtime.

However, a read-only snapshot of the dom is rendered for each scene! This allows you to catch any runtime errors caused by invalid inputs for each re-render.

# Benchmarking

This [repo](https://github.com/wavejumper/rehook-examples/tree/master/src/rehook/benchmark) benchmarks rendering todovc (found in Reagent's [examples](https://github.com/reagent-project/reagent/tree/master/examples/todomvc)) against two other implementations:

* `rehook-dom`: todomvc rewritten to use [rehook](https://github.com/wavejumper/rehook) with [rehook-dom](https://github.com/wavejumper/rehook-dom)
* `rehook-hicada`: todomvc rewritten to use [rehook](https://github.com/wavejumper/hicada) with [hicada](https://github.com/rauhs/hicada)
* `reagent`: todomvc found in Reagent's Github repo

Results:

```
reagent x 233 ops/sec ±9.95% (44 runs sampled)
rehook-dom x 223 ops/sec ±7.53% (45 runs sampled)
rehook-hicada x 489 ops/sec ±6.92% (47 runs sampled)
```

Observations:

* It looks like you gain performance by ditching the overhead of Reagent/ratoms and using React hooks
* It looks like you gain a lot of performance with Hicada's compile-time optimizations
* It looks like you lose all the performance of Hicada when you use `react-dom`, though it comes out about as fast as reagent :p

 Two things to note:
 
 * todomvc reimplementations try to stay as close to the original as possible. That means the implementations shouldn't be seen as a reference on how you should actually write a Cljs app with React hooks. 
 * In a real world React app, IMO performance boils down to cascading re-renders of child components. This will be entirely dependant on how you've modelled your data (and how your component tree is structured to consume that data). The above benchmark is incredibly naive, but nicely illustrates the performance overhead of templating.

