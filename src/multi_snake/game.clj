(ns multi-snake.game
  (:use [multi-snake.board :as ms.b]
        [multi-snake.input :as ms.in]))

(defn- pos-in-dir
  "Translate a point one unit in the provided direction.
  N.B. Origin is top-left."
  [{:keys [x y]} dir]
  (case dir
    :right {:x (inc x) :y y}
    :left  {:x (dec x) :y y}
    :down  {:x x :y (inc y)}
    :up    {:x x :y (dec y)}
    {:x x :y y}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  (let [{pos :player-pos dir :player-dir} game
        action (get-action (:input game) game)
        dir' (if action action dir)
        pos' (pos-in-dir pos dir')]
    (assoc game :player-pos pos' :player-dir dir')))

(defn make-game
  "Makes a game with the given configuration values:
  
  :starting-pos - Point to position player initially (default {0,0})
  :starting-dir - Direction player is oriented initially (default :right)
  :board        - Board to use for the game (default to board/make-board)
  :input        - Input system to use for player (default to input/basic-input)"
  ([] (make-game {}))
  ([{:keys [starting-pos starting-dir board input]
     :or {starting-pos {:x 0 :y 0}
          starting-dir :right
          board (ms.b/make-board)
          input (ms.in/basic-input)}}]
   {:player-pos starting-pos
    :player-dir starting-dir
    :board board
    :input input}))

