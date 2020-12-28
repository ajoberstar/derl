(ns dev
  (:require [clojure.tools.namespace.repl :as repl]
            [org.ajoberstar.derl.ui :as ui]))

(def app nil)

(defn start []
  (alter-var-root #'app (fn [_]
                          (ui/start)))
  :ok)

(defn stop []
  (alter-var-root #'app (fn [app]
                          (when app
                            (ui/stop app)
                            nil)))
  :ok)

(defn reset []
  (stop)
  (let [ret (repl/refresh :after `start)]
    (if (instance? Throwable ret)
      (throw ret)
      ret)))
