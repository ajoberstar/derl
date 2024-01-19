(ns org.ajoberstar.derl.ui
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [org.ajoberstar.derl.core :as core]
            [org.ajoberstar.derl.ui.effect :as effect]
            [org.ajoberstar.derl.ui.event :as event]
            [org.ajoberstar.derl.ui.view :as view])
  (:import [javafx.application Platform]))

(def initial-state
  {:repl-host "127.0.0.1"
   :repl-port 40404
   :repl-conn nil
   :repl-input "{:a 1}"
   :repl-results []
   :repl-results-scroll 0
   :status {:severity :warning
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
  (fx/unmount-renderer *state renderer)
  (reset-state))

(defn -main [& args]
  (Platform/setImplicitExit true)
  (start))

(comment
  (def app (start))
  (stop app)

  (reset-state)

  [])
