(ns org.ajoberstar.lper.ui
  (:require [cljfx.api :as fx]
            [clojure.core.async :as async]
            [clojure.core.cache :as cache]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [org.ajoberstar.lper.core :as core]))

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

(defmulti event-handler :event/type)

(defmethod event-handler ::connect [{:keys [fx/context]}]
  (let [host (fx/sub-val context :repl-host)
        port (fx/sub-val context :repl-port)
        conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:dispatch {:event/type ::status}
                  :severity :warning
                  :message "Already connected to a REPL, must disconnect before connecting again"]]
      
      (and (string? host) (int? port))
      [[:connect {:repl-host host 
                  :repl-port port}]]
      
      :else
      [[:dispatch {:event/type ::status}
                  :severity :warning
                  :message "Must provide both host and port to connect"]])))
    

(defmethod event-handler ::disconnect [{:keys [fx/context]}]
  (let [conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:disconnect {:repl-conn conn}]]
      
      :else
      [[:dispatch {:event/type ::status}
                  :severity :warning
                  :message "Not connected to a REPL, cannot disconnect"]])))
    
(defmethod event-handler ::host-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-host event)]])

(defmethod event-handler ::port-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-port (Integer/parseInt event))]])

(defmethod event-handler ::input-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-input event)]])

(defmethod event-handler ::eval [{:keys [fx/context]}]
  (let [conn (fx/sub-val context :repl-conn)
        input (fx/sub-val context :repl-input)]
    [[:eval {:repl-conn conn
             :repl-input input}]]))

(defmethod event-handler ::result [{:keys [fx/context result]}]
  [[:context (fx/swap-context context update :repl-results conj result)]])

(defmethod event-handler ::on-connection [{:keys [fx/context conn]}]
  [[:context (fx/swap-context context assoc :repl-conn conn)]])

(defmethod event-handler ::status [{:keys [fx/context severity message]}]
  [[:context (fx/swap-context context assoc :status {:severity severity
                                                     :message message})]])

(defn connect-effect [{:keys [repl-host repl-port]} dispatch!]
  (try
    (let [conn (core/connect repl-host repl-port)]
      (async/go-loop []
        (when-let [msg (async/<! (:in-chan conn))]
          (println (pr-str msg))
          (dispatch! {:event/type ::result
                      :result msg})
          (recur)))
      (dispatch! {:event/type ::on-connection
                  :conn conn})
      (dispatch! {:event/type ::status
                  :severity :info
                  :message (str "Connected to " repl-host ":" repl-port)}))
    (catch Exception e
      (dispatch! {:event/type ::status
                  :severity :error
                  :message (str "Failed to connect to " repl-host ":" repl-port " -> " (.getMessage e))}))))

(defn disconnect-effect [{:keys [repl-conn]} dispatch!]
  (try
    (core/close repl-conn)
    (dispatch! {:event/type ::on-connection
                :conn nil})
    (dispatch! {:event/type ::status
                :severity :info
                :message (str "Disconnected from REPL")})
    (catch Exception e
      (dispatch! {:event/type ::status
                  :severity :error
                  :message (str "Error disconnecting from REPL -> " (.getMessage e))}))))

(defn eval-effect [{:keys [repl-conn repl-input]} dispatch!]
  (try
    (let [_ (edn/read-string repl-input)]
      (async/>!! (:out-chan repl-conn) repl-input))
    (catch Exception e
      (dispatch! {:event/type ::status
                  :severity :error
                  :message (str "Cannot read form")})
      (dispatch! {:event/type ::result
                  :result {:tag :client-err :val (pr-str e) :exception? true}}))))

(defn root [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :title "lper"
   :width 400
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :grid-pane
                  :padding 10
                  :hgap 10
                  :column-constraints [{:fx/type :column-constraints
                                        :percent-width 100/3}
                                       {:fx/type :column-constraints
                                        :percent-width 100/3}
                                       {:fx/type :column-constraints
                                        :percent-width 100/3}]
                  :row-constraints [{:fx/type :row-constraints
                                     :percent-height 10}
                                    {:fx/type :row-constraints
                                     :percent-height 55}
                                    {:fx/type :row-constraints
                                     :percent-height 25}
                                    {:fx/type :row-constraints
                                     :percent-height 10}]
                  :children [{:fx/type :text-field
                              :grid-pane/row 0
                              :grid-pane/column 0
                              :style {:-fx-font-family "monospace"}
                              :text (fx/sub-val context :repl-host)
                              :on-text-changed {:event/type ::host-changed}}
                             {:fx/type :text-field
                              :grid-pane/row 0
                              :grid-pane/column 1
                              :style {:-fx-font-family "monospace"}
                              :text (str (fx/sub-val context :repl-port))
                              :on-text-changed {:event/type ::port-changed}}
                             {:fx/type :button
                              :grid-pane/row 0
                              :grid-pane/column 2
                              :text (if (fx/sub-val context :repl-conn)
                                      "Disconnect"
                                      "Connect")
                              :on-action {:event/type (if (fx/sub-val context :repl-conn)
                                                        ::disconnect
                                                        ::connect)}}
                             {:fx/type :scroll-pane
                              :grid-pane/row 1
                              :grid-pane/column 0
                              :grid-pane/column-span 3
                              :content {:fx/type :label
                                        :style {:-fx-font-family "monospace"}
                                        :wrap-text true
                                        :text (string/join "\n" (fx/sub-val context :repl-results))}}
                             {:fx/type :text-area
                              :grid-pane/row 2
                              :grid-pane/column 0
                              :grid-pane/column-span 3
                              :style {:-fx-font-family "monospace"}
                              :text (fx/sub-val context :repl-input)
                              :on-text-changed {:event/type ::input-changed}}
                             {:fx/type :button
                              :grid-pane/row 3
                              :grid-pane/column 0
                              :text "Eval"
                              :on-action {:event/type ::eval}}
                             {:fx/type :label
                              :grid-pane/row 3
                              :grid-pane/column 1
                              :grid-pane/column-span 2
                              :text (fx/sub-val context get-in [:status :message])}]}}})

(defn start []
  (fx/create-app
   *state
   :event-handler event-handler
   :desc-fn (fn [_] {:fx/type root})
   :co-effects {:fx/context (fx/make-deref-co-effect *state)}
   :effects {:context (fx/make-reset-effect *state)
             :dispatch fx/dispatch-effect
             :connect connect-effect
             :disconnect disconnect-effect
             :eval eval-effect}))

(defn stop [{:keys [renderer]}]
  (fx/unmount-renderer *state renderer))

(comment
  (def app (start))
  (stop app)

  [])
