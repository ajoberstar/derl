(ns org.ajoberstar.lper.ui.view
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.mutator :as mutator]
            [cljfx.prop :as prop]
            [org.ajoberstar.lper.ui.event :as event]
            [parinferish.core :as parinfer])
  (:import [java.util.function UnaryOperator]
           [javafx.scene.control ListView]))

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

(defn toolbar-pane [{:keys [fx/context]}]
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

(def list-with-scroll-props
  (fx/make-ext-with-props
   {:scroll-to (prop/make
                (mutator/setter (fn [^ListView instance ^long value]
                                  (.scrollTo instance value)))
                lifecycle/scalar)}))

(defn results-pane [{:keys [fx/context]}]
  {:fx/type list-with-scroll-props
   :props {:scroll-to (fx/sub-val context :repl-results-scroll)}
   :desc {:fx/type :list-view
          :style {:-fx-font-family "monospace"}
          :editable false
          :items (fx/sub-val context :repl-results)
          :cell-factory {:fx/cell-type :list-cell
                         :describe (fn [result]
                                     {:style {:-fx-text-fill (cond
                                                               (= :err (:tag result)) "orange"
                                                               (= :out (:tag result)) "blue"
                                                               (:exception result) "red"
                                                               :else "black")}
                                      :text (:val result)})}
          :on-scroll-to {::event/type ::event/results-scroll}}})

(defn input-pane [{:keys [fx/context]}]
  {:fx/type :text-area
   :style {:-fx-font-family "monospace"}
   :text-formatter {:fx/type :text-formatter
                    :value-converter :default
                    :value (fx/sub-val context :repl-input)
                    ;;:filter parinfer-formatter
                    :on-value-changed {::event/type ::event/input-changed}}})

(defn action-pane [{:keys [fx/context]}]
  {:fx/type :anchor-pane
   :children [{:fx/type :button
               :anchor-pane/left 0
               :text "Eval"
               :on-action {::event/type ::event/eval}}
              {:fx/type :label
               :anchor-pane/right 0
               :style {:-fx-text-fill (let [severity (fx/sub-val context get-in [:status :severity])]
                                        ({:error "red" :warning "orange" :info "black"} severity))}
               :text (fx/sub-val context get-in [:status :message])}]})

(defn root [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :title "lper"
   :width 400
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :padding 10
                  :spacing 10
                  :children [{:fx/type toolbar-pane}
                             {:fx/type results-pane
                              :v-box/vgrow :always}
                             {:fx/type input-pane}
                             {:fx/type action-pane}]}}})
