(defproject rehook/test "2.1.11"
  :description "React Hooks for Clojurescript"
  :url "https://github.com/wavejumper/rehook"

  :dependencies
  [[org.clojure/clojurescript "1.10.520" :scope "provided"]
   [org.clojure/clojure "1.10.1" :scope "provided"]
   [rehook/dom "2.1.11" :scope "provided"]
   [rehook/core "2.1.11" :scope "provided"]
   [zprint "0.5.3"]]

  :profiles
  {:cljs {:dependencies [[thheller/shadow-cljs "2.8.52"]
                         ;; https://github.com/thheller/shadow-cljs/issues/488
                         [com.google.javascript/closure-compiler-unshaded "v20190325"]
                         [org.clojure/google-closure-library "0.0-20190213-2033d5d9"]
                         [binaryage/devtools "0.9.11"]]}}

  :source-paths
  ["src"])
