(ns multi-snake.core
  (:require multi-snake.game
            multi-snake.ui)
  (:gen-class))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [game (multi-snake.game/game)]
    (multi-snake.ui/render game)))
