(ns multi-snake.core
  (:require [multi-snake.game :as ms.g]
            [multi-snake.board :as ms.b]
            [multi-snake.snake :as ms.sn]
            [multi-snake.input :as ms.in]
            [multi-snake.ui :as ms.ui])
  (:gen-class))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [config {:board (ms.b/make-board {:width 30 :height 30})
                :snakes [(ms.sn/make-snake)]
                :inputs [(ms.in/keyboard-input)]}
        game (ms.g/make-game config)]
    (ms.ui/play-game game)))

