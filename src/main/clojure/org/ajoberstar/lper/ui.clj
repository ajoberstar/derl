(ns org.ajoberstar.lper.ui
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [org.ajoberstar.lper.ui.effect :as effect]
            [org.ajoberstar.lper.ui.event :as event]
            [org.ajoberstar.lper.ui.view :as view]))

(def *state 
  (atom (fx/create-context 
         {:repl-host "127.0.0.1"
          :repl-port 40404
          :repl-conn nil
          :repl-input "{:a 1}"
          :repl-results []
          :status {:severity :info
                   :message "Not connected"}}
         cache/lru-cache-factory)))

(defn start []
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

  [])
