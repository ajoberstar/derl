(ns org.ajoberstar.derl.ui.editor.view
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]))

(def *state (atom (fx/create-context 
                    {:text "(defn foo (vector x y)
                              (+ x y 2))"
                     :form nil}
                    cache/lru-cache-factory)))

(def rainbow-fg-colors ["red" "orange" "yellow" "green" "blue" "indigo" "violet"])

(defn get-color [colors level]
  (let [index (mod level (count colors))]
    (get colors index)))

(declare form-frame)

(defn value-frame [{:keys [fx/context form nest-level]}]
  {:fx/type :label
   :style {:-fx-font-size 24}
   :text (pr-str form)})

(defn list-frame [{:keys [fx/context form nest-level]}]
  {:fx/type :h-box
   :style {:-fx-font-size 24}
  ;  :style {:-fx-border-color "black"}
   :children [{:fx/type :label
               :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
               :text "("}
              {:fx/type :h-box
              ;  :style {:-fx-border-color "purple"}
               :spacing 5
               :children (map (fn [child] 
                                {:fx/type form-frame 
                                 :form child 
                                 :nest-level (inc nest-level)}) 
                              form)}
              {:fx/type :label
               :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
               :text ")"}]})

(defn form-frame [{:keys [fx/context form nest-level]}]
  (cond
    (list? form)
    {:fx/type list-frame
     :form form
     :nest-level nest-level}
    :else
    {:fx/type value-frame
     :form form
     :nest-level nest-level}))

(defn text-buffer [{:keys [fx/context text]}]
  {:fx/type :text-area
   :text text
   :on-text-changed {:event/type ::text-buffer-change}})

(defn editor [{:keys [fx/context]}]
  {:fx/type :stage
   :showing true
   :title "DERL Editor"
   :width 1200
   :height 900
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [{:fx/type form-frame
                              :v-box/vgrow :always
                              :form (fx/sub-val context :form)
                              :nest-level 0}
                             {:fx/type text-buffer
                              :text (fx/sub-val context :text)}]}}})

(defmulti event-handler :event/type)

(defmethod event-handler ::text-buffer-change [{:keys [fx/event fx/context]}]
  (try
    (let [form (read-string event)]
      [[:context (fx/swap-context context assoc :text event)]
       [:context (fx/swap-context context assoc :form form)]])
    (catch Exception _
      [[:context (fx/swap-context context assoc :text event)]])))

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
