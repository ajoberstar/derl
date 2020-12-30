(ns org.ajoberstar.derl.alpha1.logging)

(defn log [sym level message data]
  (tap> {::level level
         ::symbol sym
         ::message message
         ::data data}))
