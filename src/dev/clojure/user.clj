(ns user)

(defn reset []
  (require 'dev)
  ((resolve 'dev/reset)))
