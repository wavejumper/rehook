# rehook

[![Clojars Project](https://img.shields.io/clojars/v/rehook.svg)](https://clojars.org/rehook)
[![CircleCI](https://circleci.com/gh/wavejumper/rehook.svg?style=svg)](https://circleci.com/gh/wavejumper/rehook)

Clojurescript React library enabling data-driven architecture

## About

rehook is built from small, modular blocks - each with an explicit notion of time, and a data-first design.

As rehook is modular, each layer builds upon the last. Each layer adds a new idea: testing, syntax, devtools, patterns.

The core library does two things:

* marry React hooks with Clojure atoms
* avoids singleton state

It is my hope that rehook's core API could be used to build general and domain-specific abstractions on top: eg re-frame, om-next style querying etc.

Its modular design, and guiding philosophy have already enabled some rich tooling already like [rehook-test](#testing). 

## Example apps

* [reax-synth](https://github.com/wavejumper/reax/tree/master/examples/synth) -- react native oscillator (demos re-frame like abstractions, integrant, etc)
* [todomvc]()
* [rehook-test]()

## Maturity

rehook is still a young project - though the core API is fairly mature. 

Through the rapid development of rehook, I have not introduced a single breaking change in the process! This is largely due to its lean surface area and modular design.

Therefore, I aim to never introduce a single breaking change to the `rehook.core` and `rehook.dom` APIs.

High level libraries like `rehook.test` -- where I am still experimenting with the design are subject to breaking change.

## Installation

The documentation assumes you are using [shadow-cljs](https://shadow-cljs.org/). 

You will need to provide your own React dependencies, eg:
```
npm install --save react
npm install --save react-dom
```

### Libraries

* rehook/core - base state/effects fns
* rehook/dom - hiccup templating DSL
* rehook/test - test library

To include one of the above libraries, add the following to your dependencies:

```
[rehook/core "2.0.1"]
```

To include all of them:

```clojure
[rehook "2.0.1"]
```

## Usage

## rehook.core

If you need a primer on React hooks, the [API docs](https://reactjs.org/docs/hooks-reference.html) are a good start.

`rehook.core` exposes 5 useful functions for state and effects:

- `use-state` convenient wrapper over `react/useState`
- `use-effect` convenient wrapper over `react/useEffect`
- `use-atom` use a Clojure atom (eg, for global app state) within a component
- `use-atom-path` like `use-atom`, except for a path into a atom (eg, `get-in`)
- `use-atom-fn` provide custom getter/setter fns to build your own abstractions

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

# Components

## rehook.dom 

`rehook.dom` provides hiccup syntax.

`rehook.dom` provides a baggage free way to pass down application context (eg, [integrant](https://github.com/weavejester/integrant) or [component](https://github.com/stuartsierra/component)) as you will see below.

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

You can opt-out of hiuccup templating by passing a third argument (the render fn) to `defui`:

```clojure
(defui no-html-macro [_ _ $]
  ($ :div {} "rehook-dom without hiccup!"))
```

Because the `$` render fn is passed into every rehook component you can overload it -- or better yet create your own custom templating syntax!

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

## Linting / editor integration

### cursive

* `rehook.dom/defui` -- resolve as defn, indentation as `indent`
* `rehook.dom/ui` -- resolve as fn, indentation as `indent`
* `rehook.test/defuitest` -- resolve as defn, indentation as `indent`
* `rehook.test/initial-render` -- indentation as `1`
* `rehook.test/next-render` -- indentation as `1`
* `rehook.test/io` -- indentation as `1`
* `rehook.test/is` -- indentation as `1`

### cljfmt

Add this to your cljfmt config:

```clojure
{}
```

### clj-kondo (calva/etc)

Add this to your `...edn` file:


## Testing

rehook allows you to test your entire application - from data layer to view.

How? Because `rehook` promotes building applications with no singleton global state. 

Therefore, you can treat your components as 'pure functions', as all inputs to the component are passed in as arguments.

rehook-test supports:

* server, react-dom and react-native
* cljs.test + nodejs target for headless/CI
* browser for devcards-like interactive development
* whatever else you can think of. it's just a function call really.

# rehook-test

## Demo

A demo report generated from rehook's own todomvc tests can be found [here](https://crowley.kibu.com.au/rehook/)
 
## Screenshots

Write tests, and get reports like this:

![image](https://i.imgur.com/OqBBSYh.png)

And headless node cljs tests!

![image](https://i.imgur.com/35ehUrd.png)

## Time-travel driven development

Writing tests for rehook is not dissimilar to how you might test with [datomic](https://www.datomic.com/) or kafka's [TopologyTestDriver](https://kafka.apache.org/11/documentation/streams/developer-guide/testing.html), with a bit of [devcards](https://github.com/bhauman/devcards) in the mix.

Each state change produces a snapshot in time that rehook captures as a 'scene'.

Like kafka's ToplogyTestDriver, the tests run in a simulated library runtime.

However, a read-only snapshot of the dom is rendered for each scene (as you can see above)! 

This allows you to catch any runtime errors caused by invalid inputs for each re-render.

## rehook.test API

**Note:** while documentation is improving, please check out the rehook tests for a reference on how to use the API.

`rehook.test` wraps the [cljs.test](https://clojurescript.org/tools/testing) API with a bit of additional syntactic sugar.

This means rehook tests compile to something `cljs.test` understands!

```clojure
(ns todo-test
  (:require [rehook.test :as rehook.test :refer-macros [defuitest is io initial-render next-render]]
            [rehook.demo.todo :as todo]))

(defuitest todo-app--clear-completed
  [scenes {:system      todo/system
           :system/args []
           :shutdown-f  identity
           :ctx-f       identity
           :props-f     identity
           :component   todo/todo-app}]

  (-> (initial-render scenes
        (is "Initial render should show 4 TODO items"
          (= (rehook.test/children :clear-completed) ["Clear completed " 4]))

        (io "Click 'Clear completed'"
          (rehook.test/invoke-prop :clear-completed :onClick [{}])))

      (next-render
       (is "After clicking 'Clear Completed', there should be no TODO items"
         (nil? (rehook.test/children :clear-completed)))))
```

We use the `->` threading macro to chain our tests.

We write tests using two basic primitives:

* `rehook.test/io` - wrapping any side-effects that will trigger a re-render (such as DOM events, HTTP calls, etc)
* `rehook.test/is` - like `cljs.test/is`, this is how we write assertions for the current render

Each test body (consisting of `is` and `io`) is scoped to a 'snapshot' of a render:

* `rehook.test/initial-render` - called on our first test
* `rehook.test/next-render` - trigger a re-render by playing any effects

## defuitest

`rehook.test/defuitest` takes in a map describing your application:

```clojure
{:system   todo/system ;; <-- your ctx constructor, eg ig/init
 :system/args [] ;; <-- any arguments to your ctx constructor
 :shutdown-f  identity ;; <-- called after the test has finished, eg ig/halt!
 :ctx-f       identity ;; <-- likely the same ctx-f passed into your applications bootstrap call
 :props-f     identity ;; <-- likely the same props-f passed into your applications bootstrap call
 :component   todo/todo-app}] ;; <-- the rehook component your are writing tests for
```

## Instrumenting the DOM

We add a **unique** key named `:rehook/id` to the props of any component we want to instrument:

```clojure
[:div {:rehook/id :my-unique-key} "I will be instrumented!"]
```

Note: this key gets compiled out when running outside of `rehook.test`!

We can then invoke props and view the props and children using the following fns:

* `rehook.test/children` - returns a collection of children
* `rehook.test/get-prop` - returns the props of the component
* `rehook.test/invoke-prop` - invokes a component's event (eg, `onClick`)

You can see these three fns in action in the demo code above.

**TODO:** provide an easy way to construct mock JS events. Perhaps look to using [jsdom](https://github.com/jsdom/jsdom)?

## Testing the data layer

* The test reports provides a way to view effects and state over time. However, this is provided only as a means of debugging. Both `use-state` and `use-effects` are implementation details - and shouldn't be tested. 
* Therefore, `rehook-test` is about testing the resulting output of the component.
* If you follow a re-frame like pattern of using global app state, it should be possible to inspect your subscriptions and invoke your effects using the `rehook.test` primitives. More documentation to follow.

## rehook.test reports

**Note**: the graphical test reporter only works for `react-dom` tests. It would be great to implement something similar for React Native (using the simulator, expo web preview, etc)!

Create a build in your `shadow-cljs.edn` file like so: 

```clojure
  {:target :browser
   :output-dir "public/js"
   :asset-path "/js"
   :closure-defines {rehook.test.browser/HTML "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"styles/todo.css\"></head><body><div></div></body></html>" ;; optional, the initial DOM html (eg, the index.html of your actual app)
                     rehook.test/target "app" ;; optional, the div id where rehook's report renders
                     rehook.test/domheight 400} ;; optional, the dom preview's iframe height
   :devtools {:before-load rehook.test/clear-registry!} ;; add this if using hot reload
   :modules {:main {:entries [rehook.test.browser
                              todo-test] ;; <-- your test nses go here...
                    :init-fn rehook.test.browser/report}}
   :release {:compiler-options {:optimizations :simple}}}
```

And you are done! 

```
shadow-cljs watch :my-build-id
```

Will render your test report. As you update your test/application code, the report will also update!

Inside of your `dev-http` folder 

## rehook.test headless

Add a build in your `shadow-cljs.edn` file like so:

```
{:target    :node-test
 :output-to "out/test.js"}
```

And you are done!

```
shadow-cljs compile :my-build-id
node out/test.js
```

Will run your headless tests

## rehook.test TODOs


