(ns org.ajoberstar.derl.ui.event
  (:require [cljfx.api :as fx])
  (:import [javafx.scene.control ScrollToEvent]))

(defmulti event-handler ::type)

(defmethod event-handler ::connect [{:keys [fx/context]}]
  (let [host (fx/sub-val context :repl-host)
        port (fx/sub-val context :repl-port)
        conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:dispatch {::type ::status
                   :severity :error
                   :message "Already connected to a REPL"}]]
      
      (and (string? host) (int? port))
      [[:connect {:repl-host host 
                  :repl-port port}]]
      
      :else
      [[:dispatch {::type ::status
                   :severity :error
                   :message "Must provide both host and port to connect"}]])))
    

(defmethod event-handler ::disconnect [{:keys [fx/context]}]
  (let [conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:disconnect {:repl-conn conn}]]
      
      :else
      [[:dispatch {::type ::status
                   :severity :error
                   :message "Not connected to a REPL"}]])))
    
(defmethod event-handler ::host-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-host event)]])

(defmethod event-handler ::port-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-port event)]])

(defmethod event-handler ::input-changed [{:keys [fx/event fx/context]}]
  [[:context (fx/swap-context context assoc :repl-input event)]])

(defmethod event-handler ::eval [{:keys [fx/context]}]
  (let [conn (fx/sub-val context :repl-conn)
        input (fx/sub-val context :repl-input)]
    (when (and conn input)
      [[:eval {:repl-conn conn
               :repl-input input}]])))

(defmethod event-handler ::clear [{:keys [fx/context]}]
  [[:context (-> context
                 (fx/swap-context assoc :repl-results [])
                 (fx/swap-context assoc :repl-results-scroll 0))]])

(defn conj-result [ctx result]
  (let [ctx (update ctx :repl-results conj result)]
    (assoc ctx :repl-results-scroll (-> ctx :repl-results count dec))))

(defmethod event-handler ::result [{:keys [fx/context result]}]
  [[:context (fx/swap-context context conj-result result)]
   (cond
     (:ms result)
     [:dispatch {::type ::status
                 :severity :info
                 :message (str "Completed in " (:ms result) "ms")}]

     (:exception result)
     [:dispatch {::type ::status
                 :severity :error
                 :message "Evaluation failed"}]

     :else
     [:dispatch {::type ::status
                 :severity :info
                 :message ""}])])

(defmethod event-handler ::results-scroll [{:keys [fx/event fx/context]}]
  (let [scroll-index (.getScrollTarget ^ScrollToEvent event)]
    [[:context (fx/swap-context context assoc :repl-results-scroll scroll-index)]]))

(defmethod event-handler ::on-connection [{:keys [fx/context conn]}]
  [[:context (fx/swap-context context assoc :repl-conn conn)]])

(defmethod event-handler ::status [{:keys [fx/context severity message]}]
  [[:context (fx/swap-context context assoc :status {:severity severity
                                                     :message message})]])
