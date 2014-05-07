(ns multi-snake.game-test
  (:use clojure.test
        [multi-snake.board :as ms.b]
        [multi-snake.input :as ms.in]
        [multi-snake.game :as ms.g]))

(deftest initialization
  (testing "Default value"
    (let [game (ms.g/make-game)
          {pos :player-pos dir :player-dir board :board} game]
      (is (= {:x 0 :y 0} pos))
      (is (= :right dir))
      (is (= (ms.b/make-board) board))))
  (testing "Parameterized values"
    (let [starting-pos {:x 5 :y 10}
          starting-dir :left
          pre-board (ms.b/make-board {:width 15})
          game (ms.g/make-game {:starting-pos starting-pos
                                :starting-dir starting-dir
                                :board pre-board})
          {pos :player-pos dir :player-dir post-board :board} game]
      (is (= starting-pos pos))
      (is (= starting-dir dir))
      (is (= pre-board post-board)))))

(deftest game-ticks
  (testing "Player moves one square in the direction they are facing"
    (let [start-pos {:x 5 :y 5}
          game-r (ms.g/tick (ms.g/make-game {:starting-dir :right
                                            :starting-pos start-pos}))
          {pos-r :player-pos} game-r
          game-d (ms.g/tick (ms.g/make-game {:starting-dir :down
                                             :starting-pos start-pos}))
          {pos-d :player-pos} game-d
          game-u (ms.g/tick (ms.g/make-game {:starting-dir :up
                                             :starting-pos start-pos}))
          {pos-u :player-pos} game-u
          game-l (ms.g/tick (ms.g/make-game {:starting-dir :left
                                             :starting-pos start-pos}))
          {pos-l :player-pos} game-l]
      (is (= {:x 6 :y 5} pos-r))
      (is (= {:x 5 :y 6} pos-d))
      (is (= {:x 5 :y 4} pos-u))
      (is (= {:x 4 :y 5} pos-l)))))

(defn- dir-input-proxy
  "Mock out the input with a proxy that returns a static value"
  [dirs]
  (let [coll (atom (cycle dirs))]
    (reify AInput
      (get-action [_ _]
        (let [dir (first @coll)]
          (swap! coll rest)
          dir)))))

(deftest user-input
  (testing "Mock input"
    (let [input (dir-input-proxy [:down])
          game (ms.g/tick (ms.g/make-game {:input input}))]
      (is (= :down (:player-dir game))))
    (let [input (dir-input-proxy [:up :left])
          game (ms.g/tick (ms.g/make-game {:input input}))
          game' (ms.g/tick game)]
      (is (= :up (:player-dir game)))
      (is (= :left (:player-dir game')))))
  (testing "Can't move in direction opposite of your current heading"
    (let [input (dir-input-proxy [:left])
          game (ms.g/tick (ms.g/make-game {:input input
                                           :starting-dir :right}))]
      (is (= :right (:player-dir game))))))

