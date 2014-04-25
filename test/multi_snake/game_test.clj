(ns multi-snake.game-test
  (:use clojure.test
        [multi-snake.board :as msb]
        [multi-snake.game :as msg]))

(deftest initialization
  (testing "Default value"
    (let [game (msg/game)
          {pos :player-pos dir :player-dir board :board} game]
      (is (= {:x 0 :y 0} pos))
      (is (= :right dir))
      (is (= (msb/board) board))))
  (testing "Parameterized values"
    (let [starting-pos {:x 5 :y 10}
          starting-dir :left
          pre-board (msb/board {:width 15})
          game (msg/game {:starting-pos starting-pos
                          :starting-dir starting-dir
                          :board pre-board})
          {pos :player-pos dir :player-dir post-board :board} game]
      (is (= starting-pos pos))
      (is (= starting-dir dir))
      (is (= pre-board post-board)))))

(deftest game-ticks
  (testing "Player moves one square in the direction they are facing"
    (let [start-pos {:x 5 :y 5}
          game-r (msg/tick (msg/game {:starting-dir :right
                                      :starting-pos start-pos}))
          {pos-r :player-pos} game-r
          game-d (msg/tick (msg/game {:starting-dir :down
                                      :starting-pos start-pos}))
          {pos-d :player-pos} game-d
          game-u (msg/tick (msg/game {:starting-dir :up
                                      :starting-pos start-pos}))
          {pos-u :player-pos} game-u
          game-l (msg/tick (msg/game {:starting-dir :left
                                      :starting-pos start-pos}))
          {pos-l :player-pos} game-l]
      (is (= {:x 6 :y 5} pos-r))
      (is (= {:x 5 :y 6} pos-d))
      (is (= {:x 5 :y 4} pos-u))
      (is (= {:x 4 :y 5} pos-l)))))

