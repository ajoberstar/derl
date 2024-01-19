(ns dev
  (:require [clojure.tools.namespace.repl :as repl]
            [org.ajoberstar.derl.ui.editor.view :as editor]))

(def app nil)

(defn start []
  (alter-var-root #'app (fn [_]
                          (editor/start)))
  :ok)

(defn stop []
  (alter-var-root #'app (fn [app]
                          (when app
                            (editor/stop app)
                            nil)))
  :ok)

(defn reset []
  (stop)
  (let [ret (repl/refresh :after `start)]
    (if (instance? Throwable ret)
      (throw ret)
      ret)))
