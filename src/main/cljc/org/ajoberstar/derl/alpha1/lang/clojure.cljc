(ns org.ajoberstar.derl.alpha1.lang.clojure
  (:require [clojure.spec.alpha :as spec]
            [org.ajoberstar.derl.alpha1.frame :as frame]
            [rewrite-clj.node :as re-node]
            [rewrite-clj.node.protocols :as re-protocols]
            [rewrite-clj.parser :as re-parser]
            [rewrite-clj.zip :as re-zip]
            #?(:cljs [cljs.reader])))

(extend-protocol frame/Frame
  re-protocols/Node
  (data [frame] (re-node/sexpr frame))
  (text [frame] (re-node/string frame)))

(defn text->frames [text]
  (re-parser/string-all text))

(defn data->frames [data]
  data)

(defn find-all [loc f p?]
  (keep identity (iterate #(re-zip/find-next % f p?) (rewrite-zip/find loc f p?))))

(defn act-on-selected [frame action]
  (let [zipper (re-zip/zipper frame)]
    (-> (re-zip/find zipper re-zip/next ::frame/selected)
        action
        (re-zip/root)
        (re-zip/node))))

(defn select [loc move-fn]
  (if (move-fn loc)
    (-> loc
        (re-zip/edit assoc ::frame/selected false)
        move-fn
        (re-zip/edit assoc ::frame/selected true))
    loc))

(defn select-prev [loc]
  (select loc re-zip/prev))

(defn select-next [loc]
  (select loc re-zip/next))

(defn select-left [loc]
  (select loc re-zip/left))

(defn select-right [loc]
  (select loc re-zip/right))

(defn select-up [loc]
  (select loc re-zip/up))

(defn select-down [loc]
  (select loc re-zip/down))

(defn select-leftmost [loc]
  (select loc re-zip/leftmost))

(defn select-rightmost [loc]
  (select loc re-zip/rightmost))

;; FIXME this won't work
(defn select-nth-level [loc n]
  (let [depth (-> loc re-zip/path count)
        ups (- depth n)]
    (if (< ups 0)
      loc
      (let [unselected (re-zip/edit loc assoc ::frame/selected false)
            unwound (nth (iterate re-zip/up unselected) ups)]
        (re-zip/edit unwound assoc ::frame/selected true)))))

(defn remove-selected [loc]
  (-> loc 
      (re-zip/remove)
      (re-zip/edit assoc ::frame/selected true)))

(defn remove-selected-to-left [loc]
  (let [remaining (re-zip/rights loc)
        old-parent (re-zip/up loc)
        new-parent (re-zip/make-node old-parent (re-zip/node old-parent) remaining)
        replaced (re-zip/replace old-parent new-parent)]
    (if (seq remaining)
      (-> replaced re-zip/down (re-zip/edit assoc ::frame/selected true))
      (re-zip/edit replaced assoc ::frame/selected true))))

(defn remove-selected-to-right [loc]
  (let [remaining (re-zip/lefts loc)
        old-parent (re-zip/up loc)
        new-parent (re-zip/make-node old-parent (re-zip/node old-parent) remaining)
        replaced (re-zip/replace old-parent new-parent)]
    (if (seq remaining)
      (-> replaced re-zip/down re-zip/rightmost (re-zip/edit assoc ::frame/selected true))
      (re-zip/edit replaced assoc ::frame/selected true))))

(defn move-selected-left [loc]
  (if (re-zip/left loc)
    (let [node (re-zip/node loc)
          left-node (re-zip/node (re-zip/left loc))
          removed (re-zip/remove loc)]
      (loop [loc removed]
        (if (= left-node (re-zip/node loc))
          (re-zip/insert-left loc node)
          (recur (re-zip/prev loc)))))
    loc))

(defn move-selected-right [loc]
  (if (re-zip/right loc)
    (let [node (re-zip/node loc)
          right-node (re-zip/node (re-zip/right loc))
          removed (re-zip/remove loc)]
      (loop [loc removed]
        (if (= right-node (re-zip/node loc))
          (re-zip/insert-right loc node)
          (recur (re-zip/next loc)))))
    loc))

(defn move-selected-leftmost [loc]
  (if (re-zip/left loc)
    (let [node (re-zip/node loc)
          leftmost-node (re-zip/node (re-zip/leftmost loc))
          removed (re-zip/remove loc)]
      (loop [loc removed]
        (if (= leftmost-node (re-zip/node loc))
          (re-zip/insert-left loc node)
          (recur (re-zip/prev loc)))))
    loc))

(defn move-selected-rightmost [loc]
  (if (re-zip/right loc)
    (let [node (re-zip/node loc)
          rightmost-node (re-zip/node (re-zip/rightmost loc))
          removed (re-zip/remove loc)]
      (loop [loc removed]
        (if (= rightmost-node (re-zip/node loc))
          (re-zip/insert-right loc node)
          (recur (re-zip/next loc)))))
    loc))

(defn move-selected-up [loc]
  (if (re-zip/up loc)
    (let [node (re-zip/node loc)
          leftmost (nil? (re-zip/left loc))
          move (if leftmost identity re-zip/up)]
      (-> loc
          (re-zip/remove)
          move
          (re-zip/insert-left node)))
    loc))

(defn move-selected-down [loc]
  (if (-> loc re-zip/next re-zip/down)
    (let [node (re-zip/node loc)]
      (-> loc
          (re-zip/remove)
          (re-zip/next)
          (re-zip/down)
          (re-zip/insert-left node)))
    loc))

(defn move-selected-previous [loc]
  (let [node (re-zip/node loc)
        removed (re-zip/remove loc)]
    (if (re-zip/branch? removed)
      (-> removed
          (re-zip/append-child node))
      (-> removed
          (re-zip/insert-right node)))))

(defn move-selected-next [loc]
  (let [node (re-zip/node loc)
        removed (re-zip/remove loc)]
    (cond
      (re-zip/end? (re-zip/next loc))
      (-> removed
          (re-zip/up)
          (re-zip/insert-right node)
          (re-zip/right))
      
      (re-zip/branch? (re-zip/next loc))
      (-> removed
          (re-zip/next)
          (re-zip/insert-child loc)
          (re-zip/down))
      
      :else
      (-> removed
          (re-zip/next)
          (re-zip/insert-right loc)
          (re-zip/right)))))

(defn clone-selected-left [loc]
  (let [node (re-zip/node loc)]
    (-> loc
        (re-zip/insert-left node)
        (select-left))))

(defn clone-selected-right [loc]
  (let [node (re-zip/node loc)]
    (-> loc
        (re-zip/insert-right node)
        (select-right))))
