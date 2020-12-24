(ns org.ajoberstar.lper.ui.view
  (:require [cljfx.api :as fx]
            [clojure.string :as string]
            [org.ajoberstar.lper.ui.event :as event]))

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
                              :on-text-changed {::event/type ::event/host-changed}}
                             {:fx/type :text-field
                              :grid-pane/row 0
                              :grid-pane/column 1
                              :style {:-fx-font-family "monospace"}
                              :text (str (fx/sub-val context :repl-port))
                              :on-text-changed {::event/type ::event/port-changed}}
                             {:fx/type :button
                              :grid-pane/row 0
                              :grid-pane/column 2
                              :text (if (fx/sub-val context :repl-conn)
                                      "Disconnect"
                                      "Connect")
                              :on-action {::event/type (if (fx/sub-val context :repl-conn)
                                                        ::event/disconnect
                                                        ::event/connect)}}
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
                              :on-text-changed {::event/type ::event/input-changed}}
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
