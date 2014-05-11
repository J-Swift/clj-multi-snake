(ns multi-snake.game-test
  (:use clojure.test)
  (:require
        [multi-snake.board :as ms.b]
        [multi-snake.input :as ms.in]
        [multi-snake.snake :as ms.sn]
        [multi-snake.game :as ms.g]))

(deftest initialization
  (testing "Default value"
    (let [{:keys [board] :as game} (ms.g/make-game)]
      (is (= #{{:x 0 :y 0}} (ms.g/positions-for-player game)))
      (is (= :right (get-in game [:snake :dir])))
      (is (= (ms.b/make-board) board))))
  (testing "Parameterized values"
    (let [starting-pos {:x 5 :y 10}
          starting-dir :left
          pre-board (ms.b/make-board {:width 15})
          game (ms.g/make-game {:starting-pos starting-pos
                                :starting-dir starting-dir
                                :board pre-board})
          {post-board :board} game]
      (is (= #{starting-pos} (ms.g/positions-for-player game)))
      (is (= starting-dir (get-in game [:snake :dir])))
      (is (= pre-board post-board)))))

(deftest game-ticks
  (testing "Player moves one square in the direction they are facing"
    (let [start-pos {:x 5 :y 5}
          game-r (ms.g/tick (ms.g/make-game {:starting-dir :right
                                            :starting-pos start-pos}))
          pos-r (get-in game-r [:snake :head])
          game-d (ms.g/tick (ms.g/make-game {:starting-dir :down
                                             :starting-pos start-pos}))
          pos-d (get-in game-d [:snake :head])
          game-u (ms.g/tick (ms.g/make-game {:starting-dir :up
                                             :starting-pos start-pos}))
          pos-u (get-in game-u [:snake :head])
          game-l (ms.g/tick (ms.g/make-game {:starting-dir :left
                                             :starting-pos start-pos}))
          pos-l (get-in game-l [:snake :head])]
      (is (= {:x 6 :y 5} pos-r))
      (is (= {:x 5 :y 6} pos-d))
      (is (= {:x 5 :y 4} pos-u))
      (is (= {:x 4 :y 5} pos-l)))))

(defn- dir-input-proxy
  "Mock out the input with a proxy that returns a static value"
  [dirs]
  (let [coll (atom (cycle dirs))]
    (reify ms.in/AInput
      (get-action [_ _]
        (let [dir (first @coll)]
          (swap! coll rest)
          dir)))))

(defn- build-game-with-proxy
  [& actions]
  (let [input (dir-input-proxy actions)]
    (ms.g/make-game {:starting-dir :right :input input})))

(deftest user-input
  (testing "Mock input"
    (let [game (ms.g/tick (build-game-with-proxy :down))]
      (is (= :down (get-in game [:snake :dir]))))
    (let [game (ms.g/tick (build-game-with-proxy :up :left))
          game' (ms.g/tick game)]
      (is (= :up (get-in game [:snake :dir])))
      (is (= :left (get-in game' [:snake :dir]))))
  (testing "Can't move in direction opposite of your current heading"
    (let [game (ms.g/tick (build-game-with-proxy :left))]
      (is (= :right (get-in game [:snake :dir])))))))

(deftest apples
  (let [game (ms.g/make-game {:starting-pos {:x 0 :y 0}
                              :starting-dir :right
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
            (is (nil? (get apples (ms.g/positions-for-player new-frame) nil)))))))
    (testing "player grows when eating an apple, but only when eating an apple"
      ; Make this a no-op to avoid situations where an apple is placed in the
      ; path of a snake during our test
      (binding [ms.g/generate-new-apples #((hash-set))]
        (is (= #{{:x 1 :y 0}, {:x 2 :y 0}} (ms.g/positions-for-player (nth frames 2))))
        (is (= #{{:x 2 :y 0}, {:x 3 :y 0}} (ms.g/positions-for-player (nth frames 3))))))))

(deftest dying
  (testing "intersecting self kills you"
    (let [input (dir-input-proxy [:right :down :left :up])
          game (ms.g/make-game {:starting-pos {:x 7 :y 0}
                                :starting-dir :right
                                :input input})
          body (into (clojure.lang.PersistentQueue/EMPTY) (for [x (range 8)]
                                                            {:x x :y 0}))
          game' (assoc game :snake (assoc (:snake game) :body body))
          frames (iterate ms.g/tick game')]
      (is (= :alive (get-in (nth frames 0) [:snake :status]))) ; initial
      (is (= :alive (get-in (nth frames 1) [:snake :status]))) ; move right
      (is (= :alive (get-in (nth frames 2) [:snake :status]))) ; move down
      (is (= :alive (get-in (nth frames 3) [:snake :status]))) ; move left
      (is (= :dead  (get-in (nth frames 4) [:snake :status]))))) ; move up
  (testing "level borders kill you"
    (let [left-wall (ms.g/make-game {:starting-pos {:x 0 :y 5}
                                     :starting-dir :left})
          top-wall (ms.g/make-game {:starting-pos {:x 5 :y 0}
                                    :starting-dir :up})
          right-wall (ms.g/make-game {:starting-pos {:x 5 :y 5}
                                      :board (ms.b/make-board {:width 6})
                                      :starting-dir :right})
          bot-wall (ms.g/make-game {:starting-pos {:x 5 :y 5}
                                    :board (ms.b/make-board {:height 6})
                                    :starting-dir :down})
          all-games [left-wall top-wall right-wall bot-wall]]
      (doseq [game all-games]
        (is (= :alive (get-in game [:snake :status]))))
      (doseq [game all-games]
        (is (= :dead (get-in (ms.g/tick game) [:snake :status])))))))


