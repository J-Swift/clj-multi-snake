(ns multi-snake.game
  (:require [clojure.set :as c.set]
            [multi-snake.board :as ms.b]
            [multi-snake.input :as ms.in]))

(defn- random-apple
  "Generate a new position for an apple given the current gamestate."
  [{{:keys [width height]} :board
    player-pos :player-pos}]
  (let [possible-apples (for [x (range width)
                              y (range height)]
                          {:x x :y y})]
    (first (drop-while #{player-pos} (shuffle possible-apples)))))

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
  "Either return the action if it is valid or the cur-dir if not."
  [action cur-dir]
  (let [opposites #{[:up :down], [:left :right]
                    [:down :up], [:right :left]}]
    (if (and action
             (not (opposites [action cur-dir])))
      action
      cur-dir)))

(defn- resolve-apples
  "If there are apples on the board, check if a player is eating them. If not,
  then generate one."
  [{:keys [player-pos apples] :as game}]
  (let [apples' (if (empty? apples)
                  #{(random-apple game)}
                  (c.set/difference apples (c.set/intersection #{player-pos} apples)))]
    (assoc game :apples apples')))

(defn- move-player
  "Fetch and resolve input, then update player position accordingly."
  [{:keys [input player-pos player-dir] :as game}]
  (let [action (ms.in/get-action input game)
        dir' (dir-for-action action player-dir)
        pos' (pos-in-dir player-pos dir')]
    (assoc game :player-pos pos' :player-dir dir')))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  (-> game
      (move-player)
      (resolve-apples)))

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

