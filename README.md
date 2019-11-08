# rehook

[![Clojars Project](https://img.shields.io/clojars/v/wavejumper/rehook.svg)](https://clojars.org/wavejumper/rehook)

35LOC library to use [React Hooks](https://reactjs.org/docs/hooks-intro.html) from Clojurescript.

Rehook provides a few useful functions for marrying Clojure's atoms together with hooks.

It exposes 5 funcitons:

- `use-state` convenient wrapper over `react/useState`
- `use-effect` convenient wrapper over `react/useEffect`
- `use-atom` use a Clojure atom (eg, for global app state) within a component
- `use-atom-path` like `use-atom`, except for a path into a atom (eg, `get-in`)
- `use-atom-fn` provide custom getter/setter fns to build your own abstractions


I talked about using React Hooks with Clojurescript at [clj-melb](https://www.meetup.com/en-AU/clj-melb/), and this [repo](https://github.com/wavejumper/rehook-examples) contains a few examples and a benchmark against Reagent.

By ditching the overhead of ratoms, rehook is able to provide a significant performance boost to Clojurescript apps :)

# Demo

```clojure
(ns demo
  (:require [rehook.core :as rehook]))

(defonce state (atom 1))

;; Example of using global app state from react component
(defn component1 []
  ;; A unique watch is added to the atom after a call to `use-atom` 
  ;; meaning, all mutations outside the component's lifecycle will
  ;; also trigger a re-render.
  ;;
  ;; When the component is unmounted, the watch is also removed. 
  (let [[val setter] (rehook/use-atom state)]
    ;; our `html` macro could come from sablono, hicada, etc
    (html
      [:div {:on-click #(setter (inc val))} (str "Increment => " val)])))

;; Example of using local app state from react component
(defn component2 []
  (let [[val setter] (rehook/use-state 1)]
    (html
      [:div {:on-click #(setter (inc val))} (str "Increment => " val)])))
```

# What templating library should I pair it with?

You can easily integrate `rehook` with existing Hiccup templating libraries like [hicada](https://github.com/rauhs/hicada) or [sablono](https://github.com/r0man/sablono). 

In the above example, the `html` macro could come from any library, it doesn't matter! All that matters is that your Clojurescript function returns a valid React element.

For this reason, `rehook` makes for a good way to migrate legacy Cljs React apps (eg, [Om classic](https://github.com/omcljs/om)) to modern React. Eg, you can very easily model the cursor abstraction with `rehook`, and not change your underlying application structure. 

I have written my own templating library [rehook-dom](https://github.com/wavejumper/rehook-dom) which pairs well with `rehook`!

# Gotchas

* When using `use-effect`, make sure the values of `deps` pass Javascript's notion of equality! 
* Enforced via convention, React hooks and effects need to be defined at the top-level of your component (and not bound conditionally) 
