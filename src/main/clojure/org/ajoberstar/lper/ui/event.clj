(ns org.ajoberstar.lper.ui.event
  (:require [cljfx.api :as fx]))

(defmulti event-handler ::type)

(defmethod event-handler ::connect [{:keys [fx/context]}]
  (let [host (fx/sub-val context :repl-host)
        port (fx/sub-val context :repl-port)
        conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:dispatch {::type ::status
                   :severity :warning
                   :message "Already connected to a REPL, must disconnect before connecting again"}]]
      
      (and (string? host) (int? port))
      [[:connect {:repl-host host 
                  :repl-port port}]]
      
      :else
      [[:dispatch {::type ::status
                   :severity :warning
                   :message "Must provide both host and port to connect"}]])))
    

(defmethod event-handler ::disconnect [{:keys [fx/context]}]
  (let [conn (fx/sub-val context :repl-conn)]
    (cond
      conn
      [[:disconnect {:repl-conn conn}]]
      
      :else
      [[:dispatch {::type ::status
                   :severity :warning
                   :message "Not connected to a REPL, cannot disconnect"}]])))
    
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
  [[:context (fx/swap-context context update :repl-results conj result)]
   (if (:ms result)
     [:dispatch {::type ::status
                 :severity :info
                 :message (str "Completed in " (:ms result) "ms")}]
     [:dispatch {::type ::status
                 :severity :info
                 :message ""}])])

(defmethod event-handler ::on-connection [{:keys [fx/context conn]}]
  [[:context (fx/swap-context context assoc :repl-conn conn)]])

(defmethod event-handler ::status [{:keys [fx/context severity message]}]
  [[:context (fx/swap-context context assoc :status {:severity severity
                                                     :message message})]])
