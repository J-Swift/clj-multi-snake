(ns multi-snake.core
  (:require [multi-snake.game :as ms.g]
            [multi-snake.board :as ms.b]
            [multi-snake.ui :as ms.ui])
  (:gen-class))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [config {:board (ms.b/make-board {:width 30 :height 30})}
        game (ms.g/make-game config)]
    (ms.ui/play-game game)))

