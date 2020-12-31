(ns org.ajoberstar.derl.alpha1.frame
  (:require [clojure.spec.alpha :as spec]
            [clojure.zip :as zip]
            [org.ajoberstar.derl.alpha1.logging :as log]))

(spec/def ::text-content string?)

(defmulti children ::type)

(defmethod children :default [_]
  nil)

(defmulti with-children (fn [frame children] (::type frame)))

(defmethod with-children :default [frame children]
  (log/log `with-children :warn "Frame type does not support adding children." {:frame frame :children children})
  frame)

(spec/def ::type keyword?)
(spec/def ::title string?)
(spec/def ::disabled? boolean?)

(spec/def ::message map?)
(spec/def ::messages (spec/coll-of ::message :kind vector?))

(defmulti frame-type ::type)

(defmethod frame-type :default [frame]
  (log/log `frame-type :debug "Using default frame keys for unknown type." {:frame frame})
  ::frame-common)

(spec/def ::frame (spec/multi-spec frame-type ::type))

(spec/def ::frame-common (spec/keys :req [::type] :opt [::title ::disabled? ::messages]))

(defmulti convert (fn [frame to-type] [(::type frame) to-type]))

(defmethod convert :default [frame to-type]
  (log/log 'convert :warn "Cannot convert frame" {:frame frame :to-type to-type})
  nil)

(defn zipper [frame]
  (zip/zipper (fn [fr] (-> fr children nil? not))
              children
              with-children
              frame))

(defn zip-to-path [zipper path]
  (loop [loc zipper
         head (first path)
         tail (rest path)]
    (cond
      (zip/end? loc)
      :not-found
      
      (= head (zip/node loc))
      (if (nil? tail)
        loc
        (recur (zip/down loc) (first tail) (rest tail)))
      
      (not= loc (-> loc zip/right zip/node))
      (recur (zip/right loc) head tail)
      
      :else
      :not-found)))
