(ns org.ajoberstar.derl.ui.editor.view
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [clojure.string :as string])
  (:import [javafx.scene.input KeyCombination KeyEvent]))

(def *state (atom (fx/create-context 
                    {:frames [{:type :list
                               :selected? true
                               :disabled? false
                               :messages []
                               :children [{:type :literal
                                           :value 'vector
                                           :selected? false
                                           :disbled? false
                                           :messages []}
                                          {:type :literal
                                           :value 'x
                                           :selected? false
                                           :disbled? false
                                           :messages []}
                                          {:type :literal
                                           :value 2
                                           :selected? false
                                           :disbled? false
                                           :messages []}]}]}
                    cache/lru-cache-factory)))

(defmulti frame->form :type)

(defmethod frame->form :list [frame]
  (apply list (map frame->form (:children frame))))

(defmethod frame->form :literal [frame]
  (:value frame))

(defmethod frame->form :default [frame]
  (throw (ex-info "Unknown frame type. Canot build form." {:frame frame})))

(def rainbow-fg-colors ["red" "orange" "yellow" "green" "blue" "indigo" "violet"])

(defn get-color [colors level]
  (let [index (mod level (count colors))]
    (get colors index)))

(declare frame-view)

(defn value-frame-view [{:keys [fx/context frame nest-level]}]
  {:fx/type :label
   :style {:-fx-font-size 24
           :-fx-border-color (if (:selected? frame) "purple" "transparent")}
   :text (pr-str (:value frame))})

(defn list-frame-view [{:keys [fx/context frame nest-level]}]
  {:fx/type :h-box
   :style {:-fx-font-size 24
           :-fx-border-color (if (:selected? frame) "purple" "transparent")}
   :children [{:fx/type :label
               :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
               :text "("}
              {:fx/type :h-box
               :spacing 5
               :children (map (fn [child] 
                                {:fx/type frame-view
                                 :frame child 
                                 :nest-level (inc nest-level)}) 
                              (:children frame))}
              {:fx/type :label
               :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
               :text ")"}]})

(defn frame-view [{:keys [fx/context frame nest-level]}]
  (cond
    (= :list (:type frame))
    {:fx/type list-frame-view
     :frame frame
     :nest-level nest-level}
    :else
    {:fx/type value-frame-view
     :frame frame
     :nest-level nest-level}))

(defn text-buffer-view [{:keys [fx/context]}]
  {:fx/type :text-area
   :editable false
   :text (fx/sub-val context (comp #(string/join "\n\n" %)
                                   #(map pr-str %)
                                   #(map frame->form %)
                                   :frames))})

(defn editor [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :title "DERL Editor"
   :width 1200
   :height 900
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :padding 10
                  :event-filter {:event/type ::editor-event-filter}
                  :children [{:fx/type :flow-pane
                              :orientation :vertical
                              :v-box/vgrow :always
                              :children (map (fn [child]
                                               {:fx/type frame-view
                                                :frame child
                                                :nest-level 0})
                                             (fx/sub-val context :frames))}
                             {:fx/type text-buffer-view}]}}})

(defmulti event-handler :event/type)

(defmethod event-handler ::text-buffer-change [{:keys [fx/event fx/context]}]
  (try
    (let [form (read-string event)]
      [[:context (fx/swap-context context assoc :text event)]
       [:context (fx/swap-context context assoc :form form)]])
    (catch Exception _
      [[:context (fx/swap-context context assoc :text event)]])))

(defmethod event-handler ::editor-event-filter [{:keys [fx/event fx/context]}]
  (when (and (instance? KeyEvent event) (= KeyEvent/KEY_RELEASED (.getEventType event)))
    (cond
      (.match (KeyCombination/valueOf "left") event)
      (println "LEFT")
      
      (.match (KeyCombination/valueOf "right") event)
      (println "RIGHT")
      
      :else
      (println "Other Key Event:" event))))

(defn start []
  (fx/create-app
   *state
   :event-handler event-handler
   :desc-fn (fn [_] {:fx/type editor})
   :co-effects {:fx/context (fx/make-deref-co-effect *state)}
   :effects {:context (fx/make-reset-effect *state)
             :dispatch fx/dispatch-effect}))

(defn stop [{:keys [renderer]}]
  (fx/unmount-renderer *state renderer))

(comment
  *e
  ,)
