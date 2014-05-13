(ns multi-snake.game
  (:require [clojure.set :as c.set]
            [multi-snake.snake :as ms.sn]
            [multi-snake.board :as ms.b]
            [multi-snake.input :as ms.in]))

(declare get-player-positions)

(def ^:dynamic generate-new-apples
  "Return a hash-set of new apple positions given the current gamestate."
  (fn [{:keys [board snakes] :as game}]
    (let [possible-apples (ms.b/get-all-cells board)
          player-positions (get-player-positions game)]
      (hash-set (first (drop-while player-positions (shuffle possible-apples)))))))

(defn- resolve-win-condition
  [{:keys [win-cond snakes] :as game}]
  (cond
    (some #{:dead} (map :status snakes)) (assoc game :status :lose)
    (win-cond game) (assoc game :status :win)
    :else game))

(defn- resolve-dead-snakes
  [{:keys [snakes] :as game}]
  (assoc game :snakes (vec (map
                                 (fn [sn]
                                   (let [hits-wall?
                                         (partial (complement ms.b/contains-point?) (:board game))]
                                   (if (or (ms.sn/intersects-self? sn)
                                           (some hits-wall? (ms.sn/snake-body-as-set sn)))
                                     (assoc sn :status :dead)
                                     sn)))
                                 snakes))))

(defn- contract-players
  "Remove the tail for the snake."
  [{:keys [snakes] :as game}]
  (let [snakes' (vec (map ms.sn/maybe-move-tail snakes))]
    (assoc game :snakes snakes')))

; TODO: clean this up
(defn- resolve-apples
  "If there are apples on the board, check if a player is eating them. If not,
  then generate one."
  [{:keys [apples snakes] :as game}]
  (if (empty? apples)
    (assoc game :apples (generate-new-apples game))
    (let [apples-eaten (fn [sn]
                         (c.set/intersection (ms.sn/snake-body-as-set sn) apples))
          apples-eaten-by-snake (map (fn [sn]
                                       (let [eaten (apples-eaten sn)]
                                         (if (empty? eaten)
                                           [sn eaten]
                                           [(ms.sn/eat-apple sn) eaten])))
                                     snakes)
          snakes' (vec (map first apples-eaten-by-snake))
          all-apples-eaten (apply c.set/union (map second apples-eaten-by-snake))]
      (assoc game :apples (c.set/difference apples all-apples-eaten) :snakes snakes'))))

(defn- extend-players-with-inputs
  "Fetch and resolve input, then update player head position accordingly."
  [game inputs]
  (assoc game :snakes (vec (map ms.sn/move-head (:snakes game) inputs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-player-positions
  "Convert player positions into something a bit more manageable."
  [game]
  (into #{} (mapcat ms.sn/snake-body-as-set (:snakes game))))

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  (let [inputs (doall (map #(ms.in/get-action % game) (:inputs game)))]
    (-> game
        (extend-players-with-inputs inputs)
        (resolve-apples)
        (contract-players)
        (resolve-dead-snakes)
        (resolve-win-condition))))

(defn make-game
  "Makes a game with the given configuration values:
  
  :snakes       - Array of snakes (default to 2 snake/make-snake)
  :inputs       - Array of inputs for snakes, associated by index
                  (default to array of input/basic-input)
  :board        - Board to use for the game (default to board/make-board)
  :apples       - Set of spaces for apples (default to #{})
  :win-cond     - Function that takes a game and returns true if the game is won
                  (default to all snakes being 5 units long)"
  ([] (make-game {}))
  ([{:keys [snakes inputs board apples win-cond]
     :or {snakes [(ms.sn/make-snake {:head {:x 0 :y 0} :dir :right})
                  (ms.sn/make-snake {:head {:x 0 :y 1} :dir :down})]
          board (ms.b/make-board)
          apples #{}
          win-cond (fn [game]
                     (every? #(>= (count (:body %)) 5)
                             (:snakes game)))}}]
  {:post [(= (count (:snakes %))
             (count (:inputs %)))]}
   {:snakes snakes
    :inputs (or inputs (repeatedly (count snakes) ms.in/basic-input))
    :board board
    :status :ongoing
    :win-cond win-cond
    :apples apples}))

