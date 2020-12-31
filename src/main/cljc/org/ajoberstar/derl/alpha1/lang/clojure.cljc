(ns org.ajoberstar.derl.alpha1.lang.clojure
  (:require [clojure.spec.alpha :as spec]
            [org.ajoberstar.derl.alpha1.frame :as frame]
            #?(:cljs [cljs.reader])))

(defmethod frame/convert [::form-content ::frame/text-content] [frame _]
  (pr-str frame))

(defmethod frame/convert [::frame/text-content ::form-content] [frame _]
  #?(:clj (read-string frame)
     :cljs (cljs.reader/read-string frame)))

(spec/def ::form-frame any?)

(spec/def ::value (not coll?))
(spec/def ::symbolic-name string?)

(spec/def ::symbolic-elements (spec/alt :stuff (spec/cat :namespace ::symbolic-name :name ::symbolic-name)
                                        :other-stuff (spec/cat :name ::symbolic-name)))
(spec/def ::collection-elements (spec/coll-of ::form-frame :kind :vector))

(derive ::scalar-frame ::form-frame)

(defmethod frame/frame-type ::scalar-frame [_]
  (spec/merge ::frame/frame-common (spec/keys :req [::value])))

(defmethod frame/convert [::scalar-frame ::form-content] [frame _]
  (::value frame))

(defmethod frame/convert [::form-content ::scalar-frame] [frame _]
  {:frame/type ::scalar-frame
   :disabled? false})

(derive ::symbolic-frame ::form-frame)
(derive ::keyword-frame ::symbolic-frame)
(derive ::symbol-frame ::symbolic-frame)

(defmethod frame/children ::symbolic-frame [frame]
  (seq (::symbolic-elements frame)))
  
(defmethod frame/with-children ::symbolic-frame [frame children]
  (assoc frame ::symbolic-elements (into [] children)))

(defmethod frame/frame-type ::symbolic-frame [_]
  (spec/merge ::frame/frame-common (spec/keys :req [::symbolic-elements])))

(defmethod frame/convert [::keyword-frame ::form-content] [frame _]
  (apply keyword (::symbolic-elements frame)))

(defmethod frame/convert [::symbol-frame ::form-content] [frame _]
  (apply symbol (::symbolic-elements frame)))

(derive ::collection-frame ::form-frame)
(derive ::list-frame ::collection-frame)
(derive ::vector-frame ::collection-frame)
(derive ::map-frame ::collection-frame)
(derive ::set-frame ::collection-frame)

(defmethod frame/children ::collection-frame [frame]
  (seq (::collection-elements frame)))

(defmethod frame/with-children ::collection-frame [frame]
  (assoc frame ::collection-elements (into [] frame)))

(defmethod frame/frame-type ::collection-frame [_]
  (spec/merge ::frame/frame-common (spec/keys :req [::collection-elements])))
