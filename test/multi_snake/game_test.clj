(ns multi-snake.game-test
  (:use clojure.test)
  (:require
        [multi-snake.board :as ms.b]
        [multi-snake.input :as ms.in]
        [multi-snake.snake :as ms.sn]
        [multi-snake.game :as ms.g]))

(deftest initialization
  (testing "Can initialize"
    (let [{:keys [board] :as game} (ms.g/make-game)]
      (is (= (ms.b/make-board) board))))
  (testing "Parameterized values"
    (let [starting-pos-1 {:x 5 :y 10}
          starting-dir-1 :left
          starting-pos-2 {:x 3 :y 2}
          starting-dir-2 :up
          snakes [(ms.sn/make-snake {:head starting-pos-1 :dir starting-dir-1})
                  (ms.sn/make-snake {:head starting-pos-2 :dir starting-dir-2})]
          pre-board (ms.b/make-board {:width 15})
          game (ms.g/make-game {:snakes snakes
                                :board pre-board})
          {post-board :board [snake1 snake2] :snakes} game]
      (is (= #{starting-pos-1} (ms.sn/snake-body-as-set snake1)))
      (is (= #{starting-pos-2} (ms.sn/snake-body-as-set snake2)))
      (is (= starting-dir-1 (:dir snake1)))
      (is (= starting-dir-2 (:dir snake2)))
      (is (= pre-board post-board)))))

(deftest game-ticks
  (testing "Player moves one square in the direction they are facing"
    (let [head {:x 5 :y 5}
          tick-snake-in-dir (fn [dir] (-> {:snakes [(ms.sn/make-snake {:dir dir
                                                                       :head head})]}
                                          (ms.g/make-game)
                                          (ms.g/tick)))
          get-snake-head (fn [game] (get-in game [:snakes 0 :head]))
          game-r (tick-snake-in-dir :right)
          pos-r (get-snake-head game-r)
          game-d (tick-snake-in-dir :down)
          pos-d (get-snake-head game-d)
          game-l (tick-snake-in-dir :left)
          pos-l (get-snake-head game-l)
          game-u (tick-snake-in-dir :up)
          pos-u (get-snake-head game-u)]
      (is (= {:x 6 :y 5} pos-r))
      (is (= {:x 5 :y 6} pos-d))
      (is (= {:x 5 :y 4} pos-u))
      (is (= {:x 4 :y 5} pos-l)))))

(defn- build-game-with-proxy
  [& actions]
  (let [input (ms.in/static-input actions)]
    (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 5 :y 5}
                                                 :dir :right})]
                     :inputs [input]})))

(deftest user-input
  (testing "Mock input"
    (let [game (ms.g/tick (build-game-with-proxy :down))]
      (is (= :down (get-in game [:snakes 0 :dir]))))
    (let [game (ms.g/tick (build-game-with-proxy :up :left))
          game' (ms.g/tick game)]
      (is (= :up (get-in game [:snakes 0 :dir])))
      (is (= :left (get-in game' [:snakes 0 :dir]))))
  (testing "Can't move in direction opposite of your current heading"
    (let [game (ms.g/tick (build-game-with-proxy :left))]
      (is (= :right (get-in game [:snakes 0 :dir])))))))

(deftest apples
  (let [game (ms.g/make-game {:snakes [(ms.sn/make-snake)]
                              :apples #{{:x 1 :y 0}}})
        frames (iterate ms.g/tick game)]
    (testing "they are consumed when a snake intersects them"
      (is (not (empty? (:apples (nth frames 0)))))
      (is (empty? (:apples (nth frames 1)))))
    (testing "new one appears one tick after another is eaten"
      (is (not (empty? (:apples (nth frames 2))))))
    (testing "new one won't appear in space currently occupied by a player"
      (let [no-apple-frame (nth frames 1)]
        (dotimes [_ 10000]
          (let [{apples :apples :as new-frame} (ms.g/tick no-apple-frame)]
            (is (= 1 (count apples)))
            (is (= :not-found (get apples (ms.g/get-player-positions new-frame) :not-found)))))))
    (testing "player grows when eating an apple, but only when eating an apple"
      ; Make this a no-op to avoid situations where an apple is placed in the
      ; path of a snake during our test
      (binding [ms.g/generate-new-apples (fn [_] (hash-set))]
        (let [frames (iterate ms.g/tick game)]
          (is (= #{{:x 1 :y 0}, {:x 2 :y 0}} (ms.sn/snake-body-as-set (get-in (nth frames 2) [:snakes 0]))))
          (is (= #{{:x 2 :y 0}, {:x 3 :y 0}} (ms.sn/snake-body-as-set (get-in (nth frames 3) [:snakes 0])))))))))

(deftest dying
  (testing "intersecting self kills you"
    (let [input (ms.in/static-input [:right :down :left :up])
          game (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 7 :y 0}
                                                            :dir :right})]
                                :inputs [input]})
          body (into (clojure.lang.PersistentQueue/EMPTY) (for [x (range 8)]
                                                            {:x x :y 0}))
          game' (assoc-in game [:snakes 0 :body] body)
          frames (iterate ms.g/tick game')
          snake-status-for-frame (fn [frame-num]
                                   (get-in (nth frames frame-num) [:snakes 0 :status]))]
      (is (= :alive (snake-status-for-frame 0))) ; initial
      (is (= :alive (snake-status-for-frame 1))) ; right
      (is (= :alive (snake-status-for-frame 2))) ; down 
      (is (= :alive (snake-status-for-frame 3))) ; left
      (is (= :dead  (snake-status-for-frame 4))))) ; up
  (testing "level borders kill you"
    (let [left-wall (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 0 :y 5}
                                                                 :dir :left})]})
          top-wall (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 5 :y 0}
                                                                :dir :up})]})
          right-wall (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 5 :y 5}
                                                                  :dir :right})]
                                      :board (ms.b/make-board {:width 6})})
          bot-wall (ms.g/make-game {:snakes [(ms.sn/make-snake {:head {:x 5 :y 5}
                                                                :dir :down})]
                                    :board (ms.b/make-board {:height 6})})
          all-games [left-wall top-wall right-wall bot-wall]]
      (doseq [game all-games]
        (is (= :alive (get-in game [:snakes 0 :status]))))
      (doseq [game all-games]
        (is (= :dead (get-in (ms.g/tick game) [:snakes 0 :status])))))))

(deftest win-condition
  (testing "changes game status to :win when it is met"
    (let [game (ms.g/make-game {:win-cond (fn [g] (= {:x 2 :y 0} (get-in g [:snakes 0 :head])))
                                :snakes [(ms.sn/make-snake {:head {:x 0 :y 0}
                                                            :dir :right})]})
          frames (iterate ms.g/tick game)]
      (is (= :ongoing (:status (nth frames 0))))
      (is (= :ongoing (:status (nth frames 1))))
      (is (= :win (:status (nth frames 2)))))))

(deftest multi-snake-refactor
  (testing "there are two snakes"
    (let [game (ms.g/make-game)
          snakes (:snakes game)]
      (is (= 2 (count snakes)))))
  (testing "they have independent inputs"
    (let [inputs [(ms.in/static-input [:down :right])
                  (ms.in/static-input [:up :left])]
          snakes [(ms.sn/make-snake {:dir :right
                                     :head {:x 1 :y 1}})
                  (ms.sn/make-snake {:dir :left
                                     :head {:x 9 :y 9}})]
          game (ms.g/make-game {:snakes snakes :inputs inputs})
          frames (iterate ms.g/tick game)]
      (is (= {:x 2 :y 2} (get-in (nth frames 2) [:snakes 0 :head])))
      (is (= {:x 8 :y 8} (get-in (nth frames 2) [:snakes 1 :head])))))
  (testing "die when they intersect each other"
    (let [snakes [(ms.sn/make-snake {:dir :right
                                     :head {:x 1 :y 0}})
                  (ms.sn/make-snake {:dir :left
                                     :head {:x 5 :y 0}})]
          game (ms.g/make-game {:snakes snakes})
          frames (iterate ms.g/tick game)
          snake-status'-for-frame (fn [frame-num]
                                    (map :status (:snakes (nth frames frame-num))))]
      (is (= [:alive :alive] (snake-status'-for-frame 0)))
      (is (= [:alive :alive] (snake-status'-for-frame 1)))
      (is (= [:dead :dead] (snake-status'-for-frame 2))))))

