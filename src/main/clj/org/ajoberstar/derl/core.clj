(ns org.ajoberstar.derl.core
  (:require [clojure.core.async :as async]
            [clojure.edn :as edn])
  (:import [java.io IOException InputStreamReader BufferedReader PushbackReader OutputStreamWriter BufferedWriter PrintWriter]
           [java.net Socket]
           [java.nio.charset StandardCharsets]))

(defrecord PreplConnection [socket rdr wrtr in-chan out-chan])

(defn prepl-reader [socket]
  (-> (.getInputStream socket)
      (InputStreamReader. StandardCharsets/UTF_8)
      (BufferedReader.)
      (PushbackReader.)))

(defn prepl-writer [socket]
  (-> (.getOutputStream socket)
      (OutputStreamWriter. StandardCharsets/UTF_8)
      (BufferedWriter.)
      (PrintWriter. true)))

(defn input-channel [rdr]
  (let [ch (async/chan 100)]
    (async/thread
      (loop []
        (tap> {:tag ::logging :message "Waiting for input..."})
        (if (try
              (when-let [input (edn/read {:eof false} rdr)]
                (tap> {:tag ::logging :message "Received input" :data input})
                (async/>!! ch input)
                true)
              (catch IOException _
                false))
          (recur)
          (tap> {:tag ::logging :message "Done looping for input"}))))
    ch))

(defn output-channel [wrtr]
  (let [ch (async/chan 3)]
    (async/go-loop []
      (tap> {:tag ::logging :message "Waiting for output..."})
      (if-let [output (async/<! ch)]
        (do
          (tap> {:tag ::logging :message "Sending output" :data output})
          (.println wrtr output)
          (recur))
        (tap> {:tag ::logging :message "Done looping for output"})))
    ch))
      
(defn connect [host port]
  (let [socket (Socket. host port)
        rdr (prepl-reader socket)
        wrtr (prepl-writer socket)
        in-chan (input-channel rdr)
        out-chan (output-channel wrtr)]
    (->PreplConnection socket rdr wrtr in-chan out-chan)))

(defn close [conn]
  (async/close! (:out-chan conn))
  (.close (:wrtr conn))
  (.close (:rdr conn))
  (async/close! (:in-chan conn))
  (.close (:socket conn)))

(comment
  (def conn (connect "localhost" 40404))
  
  (async/go-loop []
    (when-let [msg (async/<! (:in-chan conn))]
      (println (pr-str msg))
      (recur)))
  
  (async/>!! (:out-chan conn) '(str "abc" "123"))

  (async/>!! (:out-chan conn) '(+ 1 100))
  
  (async/>!! (:out-chan conn) ':repl/quit)

  (close conn)

  [])
