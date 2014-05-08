(ns multi-snake.game
  (:require [clojure.set :as c.set]
            [multi-snake.board :as ms.b]
            [multi-snake.input :as ms.in]))

(defn- pos-in-dir
  "Translate a point one unit in the provided direction.
  N.B. Origin is top-left."
  [{:keys [x y]} dir]
  (let [offsets {:right [1 0] :left [-1 0]
                 :down [0 1]  :up [0 -1]}
        offset (dir offsets)]
    {:x (+ x (get offset 0))
     :y (+ y (get offset 1))}))

(defn- dir-for-action
  "Check if the action is valid provided current direction."
  [action cur-dir]
  (let [opposites #{[:up :down], [:left :right]
                    [:down :up], [:right :left]}]
    (if (and action
             (not (opposites [action cur-dir])))
      action
      cur-dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  (let [{pos :player-pos dir :player-dir
         apples :apples input :input} game
        action (ms.in/get-action input game)
        dir' (dir-for-action action dir)
        pos' (pos-in-dir pos dir')
        apples-consumed (c.set/intersection (into #{} [pos']) apples)]
    (assoc game :player-pos pos'
                :player-dir dir'
                :apples (c.set/difference apples apples-consumed))))

(defn make-game
  "Makes a game with the given configuration values:
  
  :starting-pos - Point to position player initially (default {0,0})
  :starting-dir - Direction player is oriented initially (default :right)
  :board        - Board to use for the game (default to board/make-board)
  :input        - Input system to use for player (default to input/basic-input)
  :apples       - Set of spaces for apples (default #{})"
  ([] (make-game {}))
  ([{:keys [starting-pos starting-dir board input apples]
     :or {starting-pos {:x 0 :y 0}
          starting-dir :right
          board (ms.b/make-board)
          input (ms.in/basic-input)
          apples #{}}}]
   {:player-pos starting-pos
    :player-dir starting-dir
    :board board
    :input input
    :apples apples}))

