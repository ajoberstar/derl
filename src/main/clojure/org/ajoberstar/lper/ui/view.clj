(ns org.ajoberstar.lper.ui.view
  (:require [cljfx.api :as fx]
            [clojure.string :as string]
            [org.ajoberstar.lper.ui.event :as event]
            [parinferish.core :as parinfer])
  (:import [java.util.function UnaryOperator]))

(defn cursor [text pos]
  (loop [column 0
         line 0
         chars (seq text)]))

(def parinfer-formatter
  (reify UnaryOperator
    (apply [_ change]
           (if (.isContentChange change)
             (let [old-text (.getControlNewText change)
                   pos (.getCaretPosition change)
                   ;;[col line] (cursor old-text pos)
                   parsed (parinfer/parse old-text {:mode :smart
                                                    :cursor-column pos
                                                    :cursor-line 0})
                   new-text (parinfer/flatten parsed)]
               (println parsed)
               (doto (.clone change)
                     (.setText new-text)
                     (.setRange 0 (.length (.getControlText change)))
                     (.selectRange 1 1)))
             change))))

(defn toolbar [{:keys [fx/context]}]
  (let [connected? (boolean (fx/sub-val context :repl-conn))
        editable? (not connected?)
        button-text (if connected? "Disconnect" "Connect")
        button-event (if connected? ::event/disconnect ::event/connect)]
   {:fx/type :h-box
    :spacing 10
    :children [{:fx/type :text-field
                :style {:-fx-font-family "monospace"}
                :editable editable?
                :text (fx/sub-val context :repl-host)
                :on-text-changed {::event/type ::event/host-changed}}
               {:fx/type :text-field
                :style {:-fx-font-family "monospace"}
                :editable editable?
                :text-formatter {:fx/type :text-formatter
                                 :value-converter :long
                                 :value (fx/sub-val context :repl-port)
                                 :on-value-changed {::event/type ::event/port-changed}}}
               {:fx/type :button
                :text button-text
                :on-action {::event/type button-event}}]}))

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
                  :children [{:fx/type toolbar
                              :grid-pane/row 0
                              :grid-pane/column 0
                              :grid-pane/column-span 3}
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
                              :text-formatter {:fx/type :text-formatter
                                               :value-converter :default
                                               :value (fx/sub-val context :repl-input)
                                               ;;:filter parinfer-formatter
                                               :on-value-changed {::event/type ::event/input-changed}}}
                             {:fx/type :button
                              :grid-pane/row 3
                              :grid-pane/column 0
                              :text "Eval"
                              :on-action {::event/type ::event/eval}}
                             {:fx/type :label
                              :grid-pane/row 3
                              :grid-pane/column 1
                              :grid-pane/column-span 2
                              :text (fx/sub-val context get-in [:status :message])}]}}})
