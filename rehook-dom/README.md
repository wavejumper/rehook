# rehook-dom

[![Clojars Project](https://img.shields.io/clojars/v/wavejumper/rehook-dom.svg)](https://clojars.org/wavejumper/rehook-dom)
[![CircleCI](https://circleci.com/gh/wavejumper/rehook-dom.svg?style=svg)](https://circleci.com/gh/wavejumper/rehook-dom)

React component DSL enabling data-driven architecture

#### Hello world

```clojure
(ns demo 
  (:require 
    [rehook.dom :refer-macros [defui]]
    [react.dom.browser :as dom.browser]
    ["react-dom" :as react-dom]))

(defn system [] ;; <-- system map (this could be integrant, component, etc)
  {:dispatch #(js/console.log "TODO: implement" %)})

(defui my-component 
  [{:keys [dispatch]} ;; <-- context map from bootstrap fn
   props] ;; <-- any props passed from parent component
  [:div {:onClick #(dispatch :fire-missles)} "Fire missiles"])

(react-dom/render 
  (dom.browser/bootstrap 
    (system) ;; <-- context map
    identity ;; <-- context transformer
    clj->js ;; <-- props transformer
    my-component) ;; <-- root component
  (js/document.getElementById "myapp"))
```

## Rehook?

[rehook](https://github.com/wavejumper/rehook/) is a Clojurescript library for state management in React apps. 

It is a simple, 35LOC library that provides a [reagent](https://github.com/reagent-project/reagent) like interface for modern Cljs/React apps.

You do not need to use `rehook-dom` with `rehook`, but the two obviously pair great! 

## Why rehook-dom?

#### A baggage free way to pass down application context to components

Maybe you want to use [integrant](https://github.com/weavejester/integrant) or [component](https://github.com/stuartsierra/component) on the front end? 

One of the biggest downfalls to cljs development is the global singleton state design adopted by many libraries. 

Eg, [re-frame](https://github.com/day8/re-frame) becomes cumbersome to test, or even run multiple instances of (think devcards use case or server-side rendering) because of this pattern.

This is generally a trade-off between convenience and 'pureness'.

However `rehook-dom` gives you both! 

Via clever partial function application (and macro wizardry), the resulting DSL means you don't have to think about passing around a context map at all!

And because all `rehook-dom` components are plain Cljs fns where all inputs are arguments, you can easily test and reason about your code! Pure functions and all that.

#### Easy interop with the ReactJS ecosystem

* A Clojurescript developer should be able to simply `npm install my-react-library`, require it from the namespace and be on their way to happily using the library.
* A Clojurescript developer should be able to read the docs of `my-react-library` and intuitively map its props and API to Clojurescript.

Easy interop means you lose some Clojure idioms, but it keeps the API surface lean and obvious. 

#### server, react-dom and react-native support

There shouldn't be any difference in API, except how you render or register your root component. 

If another React target is added in the future, it should be as simple as adding another register fn for the new platform.

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

# Performance / comparisons

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

# Testing

`rehook` promotes building applications with no singleton global state.
 Therefore, you can treat your components as 'pure functions', as all inputs to the component are passed in as arguments.

Testing (with React hooks) is a deeper topic that I will explore via a blog post in the coming months. Please check back!
