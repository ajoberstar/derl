(ns org.ajoberstar.lper.ui.effect
  (:refer-clojure :exclude [eval])
  (:require [clojure.core.async :as async]
            [clojure.edn :as edn]
            [cljfx.api :as fx]
            [org.ajoberstar.lper.core :as core]
            [org.ajoberstar.lper.ui.event :as event]))

(defn connect [{:keys [repl-host repl-port]} dispatch!]
  (try
    (let [conn (core/connect repl-host repl-port)]
      (async/go-loop []
        (when-let [msg (async/<! (:in-chan conn))]
          (println (pr-str msg))
          (dispatch! {::event/type ::event/result
                      :result msg})
          (recur)))
      (dispatch! {::event/type ::event/on-connection
                  :conn conn})
      (dispatch! {::event/type ::event/status
                  :severity :info
                  :message (str "Connected to " repl-host ":" repl-port)}))
    (catch Exception e
      (dispatch! {::event/type ::event/status
                  :severity :error
                  :message (str "Failed to connect to " repl-host ":" repl-port " -> " (.getMessage e))}))))

(defn disconnect [{:keys [repl-conn]} dispatch!]
  (try
    (core/close repl-conn)
    (dispatch! {::event/type ::event/on-connection
                :conn nil})
    (dispatch! {::event/type ::event/status
                :severity :info
                :message (str "Disconnected from REPL")})
    (catch Exception e
      (dispatch! {::event/type ::event/status
                  :severity :error
                  :message (str "Error disconnecting from REPL -> " (.getMessage e))}))))

(defn eval [{:keys [repl-conn repl-input]} dispatch!]
  (try
    (let [_ (edn/read-string repl-input)]
      (async/>!! (:out-chan repl-conn) repl-input))
    (catch Exception e
      (dispatch! {::event/type ::event/status
                  :severity :error
                  :message (str "Cannot read form")})
      (dispatch! {::event/type ::event/result
                  :result {:tag :client-err :val (pr-str e) :exception true}}))))
