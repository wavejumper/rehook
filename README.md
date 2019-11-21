# rehook

## About

rehook is built from small, modular blocks - each with an explicit notion of time, and a data-first design.

As rehook is modular, each layer builds upon the last. Each layer adds a new idea: testing, syntax, devtools, patterns.

rehook's value proposition is that React is the abstraction.

The core library tries to do two things:

* marry React hooks with Clojure atoms
* avoid singleton state

If you need a primer on what React hooks, the [API docs](https://reactjs.org/docs/hooks-reference.html) are a good start.

React Hook's API shows us that functions are the ultimate interface! 

The React Hooks API already has many abstractions built on top of it, eg redux style reducers.

Therefore it is my hope that rehook's core API could be used to build general and domain-specific abstractions on top: eg re-frame impls, om-next style querying etc.

## rehook.core

`rehook.core` provides a few useful functions for marrying Clojure's atoms together with hooks.

It exposes 5 funcitons:

- `use-state` convenient wrapper over `react/useState`
- `use-effect` convenient wrapper over `react/useEffect`
- `use-atom` use a Clojure atom (eg, for global app state) within a component
- `use-atom-path` like `use-atom`, except for a path into a atom (eg, `get-in`)
- `use-atom-fn` provide custom getter/setter fns to build your own abstractions

## rehook.dom 

`rehook.dom` provides hiccup syntax.

`rehook.dom` provides a baggage free way to pass down application context (eg, [integrant](https://github.com/weavejester/integrant) or [component](https://github.com/stuartsierra/component))

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
        (js/console.log (str "Debug set to " debug)) ;; <-- the side-effect invoked after the component mounts
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