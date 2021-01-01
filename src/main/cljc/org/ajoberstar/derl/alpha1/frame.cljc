(ns org.ajoberstar.derl.alpha1.frame
  (:require [clojure.spec.alpha :as spec]))
            
(defprotocol Frame
  (data [frame])
  (text [frame]))

(spec/def ::title string?)
(spec/def ::disabled boolean?)
(spec/def ::selected boolean?)

(spec/def ::message string?)
(spec/def ::messages (spec/coll-of ::message))

(spec/def ::frame (spec/keys :opt [::title ::disabled ::selected ::messages]))
