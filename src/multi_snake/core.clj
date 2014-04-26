(ns multi-snake.core
  (:require [multi-snake.game :as ms.g]
            [multi-snake.ui :as ms.ui])
  (:gen-class))

(def FPS 10)

(defn -main
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [config {}
        initial-game (ms.g/game config)]
    (ms.ui/render-initial initial-game)
    (loop [game initial-game]
      (ms.ui/render-update game)
      (Thread/sleep (/ 1000 FPS))
      (recur (ms.g/tick game)))))

