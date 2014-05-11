(ns multi-snake.game
  (:require [clojure.set :as c.set]
            [multi-snake.snake :as ms.sn]
            [multi-snake.board :as ms.b]
            [multi-snake.input :as ms.in]))

(declare positions-for-player)

(def ^:dynamic generate-new-apples
  "Return a hash-set of new apple positions given the current gamestate."
  (fn [{{:keys [width height]} :board player-pos :player-pos :as game}]
    (let [possible-apples (for [x (range width) y (range height)]
                            {:x x :y y})]
      (hash-set (first (drop-while (positions-for-player game) (shuffle possible-apples)))))))

(defn- dir-for-action
  "Either return the action if it is valid or the cur-dir if not."
  [action cur-dir]
  (let [opposites #{[:up :down], [:left :right]
                    [:down :up], [:right :left]}]
    (if (and action
             (not (opposites [action cur-dir])))
      action
      cur-dir)))

(defn- resolve-dead-player
  [{:keys [snake] :as game}]
  ; check for intersection of body segments
  (if (some #(> % 1) (vals (frequencies (:body snake))))
    (assoc-in game [:snake :status] :dead)
    game))

(defn- contract-player
  "Remove the tail for the snake."
  [{:keys [snake] :as game}]
  (assoc game :snake (ms.sn/maybe-move-tail snake)))

(defn- resolve-apples
  "If there are apples on the board, check if a player is eating them. If not,
  then generate one."
  [{:keys [apples snake] :as game}]
  (if (empty? apples)
    (assoc game :apples (generate-new-apples game))
    (let [apples-eaten (c.set/intersection (positions-for-player game) apples)]
      (if (empty? apples-eaten)
        game
        (assoc game :apples (c.set/difference apples apples-eaten) :snake (ms.sn/eat-apple snake))))))

(defn- extend-player
  "Fetch and resolve input, then update player head position accordingly."
  [{:keys [input snake] :as game}]
  (let [action (ms.in/get-action input game)
        dir' (dir-for-action action (:dir snake))]
    (assoc game :snake (ms.sn/move-head snake dir'))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn positions-for-player
  "Convert player positions into something a bit more manageable."
  [game]
  (into #{} (get-in game [:snake :body])))

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  (-> game
      (extend-player)
      (resolve-apples)
      (contract-player)
      (resolve-dead-player)))

(defn make-game
  "Makes a game with the given configuration values:
  
  :starting-pos - Point to position player initially (default to {0,0})
  :starting-dir - Direction player is oriented initially (default to :right)
  :board        - Board to use for the game (default to board/make-board)
  :input        - Input system to use for player (default to input/basic-input)
  :apples       - Set of spaces for apples (default to #{})"
  ([] (make-game {}))
  ([{:keys [starting-pos starting-dir board input apples]
     :or {starting-pos {:x 0 :y 0}
          starting-dir :right
          board (ms.b/make-board)
          input (ms.in/basic-input)
          apples #{}}}]
   {:snake (ms.sn/make-snake {:head starting-pos :dir starting-dir})
    :board board
    :input input
    :apples apples}))

