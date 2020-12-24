(ns org.ajoberstar.lper.rebel
  (:require [rebel-readline.core :as rebel-core]
            [rebel-readline.main :as rebel-main]
            [rebel-readline.clojure.line-reader :as rebel-clj-reader]
            [rebel-readline.clojure.utils :as rebel-clj-utils]
            [rebel-readline.tools :as rebel-tools]
            [rebel-readline.utils :as rebel-utils]))


(defn create-prepl
  ([] (create-prepl nil))
  ([options] 
   (merge rebel-clj-reader/default-config
          (rebel-tools/user-config)
          options
          {:rebel})))
