(ns org.ajoberstar.lper.ui
  {:clojure.tools.namespace.repl/unload false}
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [org.ajoberstar.lper.core :as core]
            [org.ajoberstar.lper.ui.effect :as effect]
            [org.ajoberstar.lper.ui.event :as event]
            [org.ajoberstar.lper.ui.view :as view]))

(def initial-state
  {:repl-host "127.0.0.1"
   :repl-port 40404
   :repl-conn nil
   :repl-input "{:a 1}"
   :repl-results []
   :repl-results-scroll 0
   :status {:severity :info
            :message "Not connected"}})

(defonce *state (atom nil))

(defn reset-state []
  (some-> @*state
          :cljfx.context/m
          :repl-conn
          core/close)
  (reset! *state (fx/create-context 
                  initial-state
                  cache/lru-cache-factory)))

(defn start []
  (when (nil? @*state)
    (reset-state))
  (fx/create-app
   *state
   :event-handler event/event-handler
   :desc-fn (fn [_] {:fx/type view/root})
   :co-effects {:fx/context (fx/make-deref-co-effect *state)}
   :effects {:context (fx/make-reset-effect *state)
             :dispatch fx/dispatch-effect
             :connect effect/connect
             :disconnect effect/disconnect
             :eval effect/eval}))

(defn stop [{:keys [renderer]}]
  (fx/unmount-renderer *state renderer))

(comment
  (def app (start))
  (stop app)

  (reset-state)

  [])
