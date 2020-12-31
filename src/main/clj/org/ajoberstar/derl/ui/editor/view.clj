(ns org.ajoberstar.derl.ui.editor.view
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [clojure.string :as string]
            [clojure.zip :as zip])
  (:import [javafx.scene.input KeyCombination KeyEvent]))

(def *state (atom (fx/create-context 
                    {:frame-root {:type :buffer
                                  :selected? false
                                  :disabled? false
                                  :messages []
                                  :children [{:type :list
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
                                                          :messages []}]}]}}
                    cache/lru-cache-factory)))

(defn frame-zipper [frame]
  (zip/zipper (fn [fr] (not= :value fr))
              (comp seq :children)
              (fn [frame children]
                (assoc frame :children (into [] children)))
              frame))

(defn zip-to-selected [zipper]
  (loop [loc zipper]
    (cond
      (zip/end? loc)
      (zip/root loc)

      (:selected? (zip/node loc))
      loc

      :else
      (recur (zip/next loc)))))

(defn select-left [loc]
  (if (= loc (zip/leftmost loc))
    loc
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/left)
        (zip/edit assoc :selected? true))))

(defn select-right [loc]
  (if (= loc (zip/rightmost loc))
    loc
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/right)
        (zip/edit assoc :selected? true))))

(defn select-leftmost [loc]
  (if (= loc (zip/leftmost loc))
    loc
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/leftmost)
        (zip/edit assoc :selected? true))))

(defn select-rightmost [loc]
  (if (= loc (zip/rightmost loc))
    loc
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/rightmost)
        (zip/edit assoc :selected? true))))

(defn select-up [loc]
  (if (zip/up loc)
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/up)
        (zip/edit assoc :selected? true))
    loc))

(defn select-down [loc]
  (if (zip/down loc)
    (-> loc
        (zip/edit assoc :selected? false)
        (zip/down)
        (zip/edit assoc :selected? true))
    loc))

(defn select-nth-level [loc n]
  (let [depth (-> loc zip/path count)
        ups (- depth n)]
    (if (< ups 0)
      loc
      (let [unselected (zip/edit loc assoc :selected? false)
            unwound (nth (iterate zip/up unselected) ups)]
        (zip/edit unwound assoc :selected? true)))))

(defn remove-selected [loc]
  (-> loc 
      (zip/remove)
      (zip/edit assoc :selected? true)))

(defn remove-selected-to-left [loc]
  (let [boundary (zip/leftmost loc)
        edited (-> loc
                   (zip/remove)
                   (zip/edit assoc :selected? true))]
    (if (= loc boundary)
      edited
      (recur edited))))

(defn remove-selected-to-right [loc]
  (let [boundary (zip/rightmost loc)
        move (if (= loc boundary) identity zip/right)
        edited (-> loc
                   (zip/remove)
                   move
                   (zip/edit assoc :selected? true))]
    (if (= loc boundary)
      edited
      (recur edited))))

(defn move-selected-left [loc]
  (if (= loc (zip/leftmost loc))
    loc
    (let [node (zip/node loc)]
      (-> loc
          (zip/remove)
          (zip/insert-left node)
          (zip/left)))))

(defn move-selected-right [loc]
  (if (= loc (zip/rightmost loc))
    loc
    (let [node (zip/node loc)]
      (-> loc
          (zip/remove)
          (zip/next)
          (zip/insert-right node)
          (zip/right)))))

(defn move-selected-leftmost [loc]
  (if (= loc (zip/leftmost loc))
    loc
    (let [node (zip/node loc)]
      (-> loc
          (zip/remove)
          (zip/leftmost)
          (zip/insert-left node)
          (zip/left)))))

(defn move-selected-rightmost [loc]
  (if (= loc (zip/rightmost loc))
    loc
    (let [node (zip/node loc)]
      (-> loc
          (zip/remove)
          (zip/next)
          (zip/rightmost)
          (zip/insert-right node)
          (zip/right)))))

(defn move-selected-up [loc]
  (if (zip/up loc)
    (let [node (zip/node loc)
          leftmost (nil? (zip/left loc))
          move (if leftmost identity zip/up)]
      (-> loc
          (zip/remove)
          move
          (zip/insert-left node)
          (zip/left)))
    loc))

(defn move-selected-down [loc]
  (if (-> loc zip/next zip/down)
    (let [node (zip/node loc)]
      (-> loc
          (zip/remove)
          (zip/next)
          (zip/down)
          (zip/insert-left node)
          (zip/left)))
    loc))

(defn clone-selected-left [loc]
  (let [node (zip/node loc)]
    (-> loc
        (zip/insert-left node)
        (select-left))))

(defn clone-selected-right [loc]
  (let [node (zip/node loc)]
    (-> loc
        (zip/insert-right node)
        (select-right))))

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

(defn insert-frame-view [{:keys [fx/context frame nest-level]}]
  {:fx/type :label
   :style {:-fx-font-size 24
           :-fx-border-color (if (:selected? frame) "purple" "transparent")}
   :text " "})

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

(defn buffer-frame-view [{:keys [fx/context frame]}]
  {:fx/type :flow-pane
   :style {:-fx-border-color (if (:selected? frame) "purple" "transparent")}
   :orientation :vertical
   :children (map (fn [child]
                    {:fx/type frame-view
                     :frame child
                     :nest-level 0})
                  (:children frame))})

(defn frame-view [{:keys [fx/context frame nest-level]}]
  (cond
    (= :insert (:type frame))
    {:fx/type insert-frame-view
     :frame frame}

    (= :buffer (:type frame))
    {:fx/type buffer-frame-view
     :frame frame}

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
                  :children [{:fx/type frame-view
                              :v-box/vgrow :always
                              :frame (fx/sub-val context :frame-root)}
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
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :left}]]

      (.match (KeyCombination/valueOf "right") event)
      [[:select-move {:frame-root (fx/sub-val context :frame-root)
                      :direction :right}]]

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

(defmethod event-handler ::on-selection-change [{:keys [fx/context frame-root]}]
  [[:context (fx/swap-context context assoc :frame-root frame-root)]])

(defn select-move [{:keys [frame-root direction]} dispatch!]
  (let [loc (zip-to-selected (frame-zipper frame-root))
        edited (cond
                 (= :left direction)
                 (select-left loc)

                 (= :right direction)
                 (select-right loc)

                 (= :leftmost direction)
                 (select-leftmost loc)

                 (= :rightmost direction)
                 (select-rightmost loc)

                 (= :up direction)
                 (select-up loc)

                 (= :down direction)
                 (select-down loc)

                 :else
                 (throw (ex-info "Invalid direction" {:direction direction})))
        new-root (-> edited zip/root)]
    (dispatch! {:event/type ::on-selection-change
                :frame-root new-root})))

(defn select-level [{:keys [frame-root level]} dispatch!]
  (let [loc (zip-to-selected (frame-zipper frame-root))
        edited (select-nth-level loc level)
        new-root (-> edited zip/root)]
    (dispatch! {:event/type ::on-selection-change
                :frame-root new-root})))

(defn move-selected [{:keys [frame-root direction]} dispatch!]
  (let [loc (zip-to-selected (frame-zipper frame-root))
        edited (cond
                 (= :left direction)
                 (move-selected-left loc)

                 (= :right direction)
                 (move-selected-right loc)

                 (= :leftmost direction)
                 (move-selected-leftmost loc)

                 (= :rightmost direction)
                 (move-selected-rightmost loc)

                 (= :up direction)
                 (move-selected-up loc)

                 (= :down direction)
                 (move-selected-down loc)

                 :else
                 (throw (ex-info "Invalid direction" {:direction direction})))
        new-root (-> edited zip/root)]
    (dispatch! {:event/type ::on-selection-change
                :frame-root new-root})))

(defn clone-selected [{:keys [frame-root direction]} dispatch!]
  (let [loc (zip-to-selected (frame-zipper frame-root))
        edited (cond
                 (= :left direction)
                 (clone-selected-left loc)

                 (= :right direction)
                 (clone-selected-right loc)

                 :else
                 (throw (ex-info "Invalid direction" {:direction direction})))
        new-root (-> edited zip/root)]
    (dispatch! {:event/type ::on-selection-change
                :frame-root new-root})))

(defn do-remove-selected [{:keys [frame-root target]} dispatch!]
  (let [loc (zip-to-selected (frame-zipper frame-root))
        edited (cond
                 (= :current target)
                 (remove-selected loc)
                 
                 (= :left target)
                 (remove-selected-to-left loc)
                 
                 (= :right target)
                 (remove-selected-to-right loc))
        new-root (-> edited zip/root)]
    (dispatch! {:event/type ::on-selection-change
                :frame-root new-root})))

(defn start []
  (fx/create-app
   *state
   :event-handler event-handler
   :desc-fn (fn [_] {:fx/type editor})
   :co-effects {:fx/context (fx/make-deref-co-effect *state)}
   :effects {:context (fx/make-reset-effect *state)
             :dispatch fx/dispatch-effect
             :select-move select-move
             :select-level select-level
             :move-selected move-selected
             :clone-selected clone-selected
             :remove-selected do-remove-selected}))

(defn stop [{:keys [renderer]}]
  (fx/unmount-renderer *state renderer))

(comment
  *e

  (-> @*state :cljfx.context/m)

  (def z (frame-zipper (-> @*state :cljfx.context/m :frame-root)))

  (zip-to-selected z)

  (-> z select-down zip/root)

  (-> z zip-to-selected zip/remove zip/up (zip/insert-left {:my-node "yeay"}) (zip/left))
  (-> @*state :cljfx.context/m :frame-root frame-zipper zip-to-selected move-selected-up)
  ,)
