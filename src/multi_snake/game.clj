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
          player-positions (get-player-positions game)
          ; 1/4 of the apples are 'owned'
          owner (->> snakes
                     (mapcat (fn [sn] [:all :all :all (:id sn)]))
                     shuffle
                     first)
          apple (->> possible-apples
                     shuffle
                     (drop-while player-positions)
                     first)]
      (hash-set (assoc apple :edible-by owner)))))

(defn- resolve-win-condition
  [{:keys [win-cond snakes] :as game}]
  (cond
    (some #{:dead} (map :status snakes)) (assoc game :status :lose)
    (win-cond game) (assoc game :status :win)
    :else game))

(defn- resolve-dead-snakes
  [{:keys [snakes] :as game}]
  (let [snake-bodies (zipmap (range) (map ms.sn/snake-body-as-set snakes))
        hits-wall? (partial (complement ms.b/contains-point?) (:board game))
        hits-other-snake? (fn [body others]
                            (some (complement empty?)
                                 (c.set/intersection body
                                                     (into #{} (apply concat others)))))
        snakes' (->> snakes
                   (map-indexed
                       (fn [idx sn]
                         (let [body (snake-bodies idx)]
                           (if (or (ms.sn/intersects-self? sn)
                                   (some hits-wall? body)
                                   (hits-other-snake? body (vals (dissoc snake-bodies idx))))
                             (assoc sn :status :dead)
                             sn))))
                   vec)]
    (assoc game :snakes snakes')))

(defn- contract-players
  [{:keys [snakes] :as game}]
  (let [snakes' (vec (map ms.sn/maybe-move-tail snakes))]
    (assoc game :snakes snakes')))

(defn- resolve-eaten-apples
  [{:keys [apples snakes] :as game}]
  (let [apples-edible-by-snake (fn [sn]
                                 (filter (fn [apple]
                                           (let [eater (get apple :edible-by :all)]
                                             (#{:all (:id sn)} eater)))
                                         apples))
        apples-eaten-by-snake (fn [sn]
                                (let [body (ms.sn/snake-body-as-set sn)]
                                  (filter #(body {:x (:x %) :y (:y %)})
                                          (apples-edible-by-snake sn))))
        snakes-with-apples-eaten (map (fn [sn]
                                     (let [eaten (apples-eaten-by-snake sn)]
                                       (if (empty? eaten)
                                         [sn eaten]
                                         [(ms.sn/eat-apple sn) eaten])))
                                   snakes)
        snakes' (vec (map first snakes-with-apples-eaten))
        all-apples-eaten (apply c.set/union (map second snakes-with-apples-eaten))]
    (assoc game :apples (c.set/difference apples all-apples-eaten) :snakes snakes')))

(defn- resolve-apples
  [{:keys [apples snakes] :as game}]
  (if (empty? apples)
    (assoc game :apples (generate-new-apples game))
    (resolve-eaten-apples game)))

(defn- extend-players-with-inputs
  [game inputs]
  (assoc game :snakes (vec (map ms.sn/move-head (:snakes game) inputs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-player-positions
  "Convert player positions into something a bit more manageable."
  [game]
  (into #{} (mapcat :body (:snakes game))))

(defn tick
  "Takes a gamestate and moves it one unit-of-time forward, returning the
  resultant gamestate."
  [game]
  ; Do this now so that the main loop is mostly side-effect free (still have
  ; a non-deterministic new-apple-fn)
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
   {:snakes (vec (map #(assoc %1 :id %2) snakes (range)))
    :inputs (or inputs (repeatedly (count snakes) ms.in/basic-input))
    :board board
    :status :ongoing
    :win-cond win-cond
    :apples (into #{} apples)}))

