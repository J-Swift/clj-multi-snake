(ns multi-snake.core
  (:require multi-snake.board
            multi-snake.ui)
  (:gen-class))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [board (multi-snake.board/board)]
    (multi-snake.ui/render board)))
