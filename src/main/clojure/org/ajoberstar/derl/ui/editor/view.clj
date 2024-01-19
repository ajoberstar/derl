(ns org.ajoberstar.derl.ui.editor.view
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [org.ajoberstar.derl.alpha1.frame :as frame]
            [org.ajoberstar.derl.alpha1.lang.clojure :as lang-clj]
            [rewrite-clj.node :as re-node]
            [rewrite-clj.zip :as re-zip])
  (:import [javafx.scene.input KeyCombination KeyEvent]))

(def *state (atom (fx/create-context
                   {:text-input "(defn foo [x y]
                                      (+ x y 2))
                                    
                                    (defn bar [x & rest]
                                      (let [z (* x 2)]
                                        (into [z] (comp (map inc)
                                                        (filter even?)) 
                                              rest)))"
                    :text-output ""
                    :frame-root nil}
                   cache/lru-cache-factory)))

(def rainbow-fg-colors ["red" "orange" "yellow" "green" "blue" "indigo" "violet"])

(defn get-color [colors level]
  (let [index (mod level (count colors))]
    (get colors index)))

(declare frame-view)

(defn insert-frame-view [{:keys [fx/context frame selected? nest-level]}]
  {:fx/type :label
   :style {:-fx-font-size 24
           :-fx-border-color (if selected? "purple" "transparent")}
   :text " "})

(defn node-frame-view [{:keys [fx/context frame nest-level]}]
  (if (re-node/inner? frame)
    {:fx/type :h-box
     :style {:-fx-font-size 24
             :-fx-border-color (if (::frame/selected frame) "purple" "transparent")}
     :children [{:fx/type :label
                 :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
                 :text (re-node/sexpr frame)}
                {:fx/type :h-box
                 :spacing 5
                 :children (map (fn [child]
                                  {:fx/type node-frame-view
                                   :frame child
                                   :nest-level (inc nest-level)})
                                frame)}
                {:fx/type :label
                 :style {:-fx-text-fill (get-color rainbow-fg-colors nest-level)}
                 :text (re-node/sexpr frame)}]}
    {:fx/type :label
     :style {:-fx-font-size 24
             :-fx-border-color (if (::frame/selected frame) "purple" "transparent")}
     :text (re-node/sexpr frame)}))

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
                  :children [{:fx/type node-frame-view
                              :v-box/vgrow :always
                              :frame (fx/sub-val context :frame-root)
                              :next-level 0}
                             {:fx/type :h-box
                              :children [{:fx/type :text-area
                                          :editable false
                                          :text (fx/sub-val context :text-input)
                                          :on-text-changed {:event/type ::on-text-input}}
                                         {:fx/type :text-area
                                          :editable false
                                          :text (fx/sub-val context (comp frame/text :frame-root))}]}]}}})

(defmulti event-handler :event/type)

(defmethod event-handler ::on-text-input [{:keys [fx/event fx/context]}]
  (try
    (let [node (lang-clj/text->frames event)
          frame (assoc node ::frame/selected true)]
      [[:context (fx/swap-context context assoc :text-input frame)]
       [:context (fx/swap-context context assoc :frame-root frame)]])))

(defmethod event-handler ::editor-event-filter [{:keys [fx/event fx/context]}]
  (when (and (instance? KeyEvent event) (= KeyEvent/KEY_RELEASED (.getEventType event)))
    (cond
      (.match (KeyCombination/valueOf "left") event)
      [[:context (fx/swap-context context assoc :frame-root (lang-clj/act-on-selected (fx/sub-val context :frame-root)
                                                                                      lang-clj/select-left))]]

      (.match (KeyCombination/valueOf "right") event)
      [[:context (fx/swap-context context assoc :frame-root (lang-clj/act-on-selected (fx/sub-val context :frame-root)
                                                                                      lang-clj/select-right))]]

      (.match (KeyCombination/valueOf "up") event)
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :up}]]

      (.match (KeyCombination/valueOf "down") event)
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :down}]]

      (.match (KeyCombination/valueOf "ctrl+left") event)
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :leftmost}]]

      (.match (KeyCombination/valueOf "ctrl+right") event)
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :rightmost}]]

      (.match (KeyCombination/valueOf "alt+left") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :left}]]

      (.match (KeyCombination/valueOf "alt+right") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :right}]]

      (.match (KeyCombination/valueOf "alt+up") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :up}]]

      (.match (KeyCombination/valueOf "alt+down") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :down}]]

      (.match (KeyCombination/valueOf "ctrl+alt+left") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :leftmost}]]

      (.match (KeyCombination/valueOf "ctrl+alt+right") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :rightmost}]]

      (.match (KeyCombination/valueOf "alt+shift+left") event)
      [[:clone-selected {:frame-root (fx/sub-val context :frame-root)
                         :direction :left}]]

      (.match (KeyCombination/valueOf "alt+shift+right") event)
      [[:clone-selected {:frame-root (fx/sub-val context :frame-root)
                         :direction :right}]]

      (.match (KeyCombination/valueOf "1") event)
      [[:select-level {:frame-root (fx/sub-val context :frame-root)
                       :level 0}]]

      (.match (KeyCombination/valueOf "2") event)
      [[:select-level {:frame-root (fx/sub-val context :frame-root)
                       :level 1}]]

      (.match (KeyCombination/valueOf "3") event)
      [[:select-level {:frame-root (fx/sub-val context :frame-root)
                       :level 2}]]

      (.match (KeyCombination/valueOf "4") event)
      [[:select-level {:frame-root (fx/sub-val context :frame-root)
                       :level 3}]]

      (.match (KeyCombination/valueOf "tab") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :previous}]]

      (.match (KeyCombination/valueOf "shift:tab") event)
      [[:move-selected {:frame-root (fx/sub-val context :frame-root)
                        :direction :next}]]

      (or (.match (KeyCombination/valueOf "backspace") event)
          (.match (KeyCombination/valueOf "delete") event)
          (.match (KeyCombination/valueOf "d") event))
      [[:remove-selected {:frame-root (fx/sub-val context :frame-root)
                          :target :current}]]

      (.match (KeyCombination/valueOf "ctrl+backspace") event)
      [[:remove-selected {:frame-root (fx/sub-val context :frame-root)
                          :target :left}]]

      (.match (KeyCombination/valueOf "ctrl+delete") event)
      [[:remove-selected {:frame-root (fx/sub-val context :frame-root)
                          :target :right}]])))

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
