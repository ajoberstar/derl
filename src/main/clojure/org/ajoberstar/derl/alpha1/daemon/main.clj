(ns org.ajoberstar.derl.alpha1.daemon.main
  (:require [clojure.core.server :as server]
            [com.stuartsierra.component :as comp]))

(defn meta-component [component {:keys [status start stop]}]
  (with-meta component
    {`comp/start (fn [c]
                   (if (= :started (status c))
                     c
                     (start c)))
     `comp/stop (fn [c]
                  (if (= :stopped (status c))
                    c
                    (stop c)))}))

(defn daemon-component [address port]
  (meta-component
   {::address address
    ::port port}
   {:start (fn [component]
             (let [server (server/start-server
                           {:name "DERL Daemon"
                            :address address
                            :port port
                            :accept server/io-prepl
                            :server-daemon false})]
               (assoc component ::server server)))
    :stop (fn [component]
            (server/stop-server (::server component))
            (dissoc component ::server))
    :status (fn [component]
              (if (::server component)
                :started
                :stopped))}))

(defn system-map [{:keys [address port] :or {address "127.0.0.1" port "60606"}}]
  (comp/system-map
   :org.ajoberstar.derl.alpha1.daemon (daemon-component address (Integer/parseInt port))))

(def system (atom nil))

(declare stop)

(defn init [opts]
  (stop)
  (reset! system (system-map opts)))

(defn start []
  (swap! system comp/start))

(defn stop []
  (swap! system (fn [s] (when s (comp/stop s)))))

(defn -main [& args]
  (init {})
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop)))

(comment
  (init {})
  (start)
  (stop)

  [])
