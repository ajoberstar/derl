(ns org.ajoberstar.lper.rebel.clj-prepl
  (:require [clojure.core.async :as async]
            [org.ajoberstar.lper.core :as core]
            [rebel-readline.core :as rebel-core]
            [rebel-readline.main :as rebel-main]
            [rebel-readline.clojure.line-reader :as rebel-clj-reader]
            [rebel-readline.clojure.utils :as rebel-clj-utils]
            [rebel-readline.tools :as rebel-tools]
            [rebel-readline.utils :as rebel-utils]))

(def)

(defmethod rebel-clj-reader/-current-ns ::service [service])

(defmethod rebel-clj-reader/-complete ::service [service word options])

(defmethod rebel-clj-reader/-resolve-meta ::service [service var-str])

(defmethod rebel-clj-reader/-source ::service [service var-str])

(defmethod rebel-clj-reader/-apropos ::service [service var-str])

(defmethod rebel-clj-reader/-doc ::service [service var-str])

(defmethod rebel-clj-reader/-read-string ::service [service form-str])

(defmethod rebel-clj-reader/-eval ::service [service form])

(defn create
  ([] (create nil))
  ([options]
   (let [service (merge rebel-clj-reader/default-config
                        (rebel-tools/user-config)
                        options
                        {:rebel-readline.service/type ::service})]
     (assoc service ::connection (core/connect (:host service) (:port service))))))

(defn close [service]
  (core/close (::connection service)))
