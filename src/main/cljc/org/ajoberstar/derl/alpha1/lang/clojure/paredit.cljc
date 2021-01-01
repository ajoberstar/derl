(ns org.ajoberstar.derl.alpha1.lang.clojure.paredit)


;; Create node
(defn paredit-open-round [ctx loc])
(defn paredit-open-square [ctx loc])
(defn paredit-open-curly [ctx loc])
(defn paredit-open-angle [ctx loc])
(defn paredit-open-double-quote [ctx loc])

;; Close node (do I need this)
(defn paredit-close-round [ctx loc])
(defn paredit-close-square [ctx loc])
(defn paredit-close-curly [ctx loc])
(defn paredit-close-angle [ctx loc])

;; Wrap in node
(defn paredit-wrap-round [ctx loc])
(defn paredit-wrap-square [ctx loc])
(defn paredit-wrap-curly [ctx loc])
(defn paredit-wrap-angle [ctx loc])
(defn paredit-wrap-double-quote [ctx loc])

;; Deletes without unbalancing parens
(defn paredit-forward-delete [ctx loc])
(defn paredit-forward-kill-word [ctx loc])
(defn paredit-backward-delete [ctx loc])
(defn paredit-backward-kill-word [ctx loc])
;; Kill to end (i.e. kill rights)
(defn paredit-kill [ctx loc])

;; Forward to next close paren and move closer
;; Can reinterpret as move to rightmost remove then up/insert-right
(defn paredit-forward-slurp-sexp [ctx loc])
;; Backward to prev open paren and move away
;; Can reinterpret as up/left/remove/next/insert-child (with special accounting for the remove)
(defn paredit-backward-slurp-sexp [ctx loc])
;; Forward to next close paren and push away
;; Can reinterpret as up/right/remove/append-child
(defn paredit-forward-barf-sexp [ctx loc])
;; Backward to prev open paren and move closer
;; Can reinterpret as move to leftmost remove then up/insert-left
(defn paredit-backward-barf-sexp [ctx loc])

;; Move right 
(defn paredit-forward [ctx loc])
(defn paredit-backward [ctx loc])

(defn paredit-forward-down [ctx loc])
(defn paredit-backward-up [ctx loc])

(defn paredit-slice-sexp [ctx loc])
(defn paredit-slice-sexp-killing-backward [ctx loc])
(defn paredit-slice-sexp-killing-forward [ctx loc])

(defn paredit-split-sexp [ctx loc])
(defn paredit-join-sexp [ctx loc])
