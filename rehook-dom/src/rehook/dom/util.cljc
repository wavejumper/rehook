(ns rehook.dom.util
  (:require [clojure.string :as str]))

(defn merge-arguments
  [args extra-args]
  (let [{:keys [className id]} extra-args]
    (cond-> args
      (or id (:id args))
      (update :id #(or id %))

      (or (:className args) className)
      (update :className #(if (and % className)
                            (str % " " className)
                            (or % className))))))

(def keyword->elem
  (memoize
   (fn [kw]
     (let [elem (name kw)]
       (if (or (str/includes? elem ".") (str/includes? elem "#"))
         (let [[elem & class-names] (str/split elem #"\.")
               [elem id] (str/split elem #"#")
               [id class-names] (if id
                                  [id class-names]
                                  (let [classes-and-id (map #(str/split % #"#") class-names)]
                                    [(first (map second classes-and-id)) (map first classes-and-id)]))]
           [elem (cond-> {}
                   (not-empty class-names) (assoc :className (str/join " " class-names))
                   id (assoc :id id))])
         [elem {}])))))