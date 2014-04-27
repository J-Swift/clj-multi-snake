(ns multi-snake.core
  (:require [multi-snake.game :as ms.g]
            [multi-snake.ui :as ms.ui])
  (:gen-class))

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [config {:board {:width 30 :height 30}}
        game (ms.g/game config)]
    (ms.ui/play-game game)))

