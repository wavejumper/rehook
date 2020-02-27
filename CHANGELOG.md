## 2.1.5 (Feburary 27, 2020)

### rehook-dom

* Add `rehook.util/children` fn to access child elements of rehook component
* react-dom/react-native: rehook components expose a human readable `displayName` for better debugging
* Support fragments via `:>` shorthand
* Support shorthand class/id notation, eg `:div.foo#bar`
* Third argument to component can also be a seq (eg,`[:div {} (for [x y] ...)]`)
* Fix more edge cases in complex, nested hiccup compilation

## 2.1.4 (Feburary 11, 2020)

### rehook-dom

* Fix more edge cases in complex, nested hiccup compilation

## 2.1.3 (Feburary 11, 2020)

### rehook-dom

* Fix hiccup compilation bug when compiling symbols

## 2.1.2 (Feburary 11, 2020)

### rehook-dom

* Add `rehook.util/react-props` helper fn

## 2.1.1 (Feburary 11, 2020)

### rehook-dom

* Pass React props as metadata with key `:react/props` into Rehook components.

## 2.1.0 (Feburary 11, 2020)

### rehook-dom

* **Breaking:** leave props untouched when passed into other Rehook components.