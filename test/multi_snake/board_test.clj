(ns multi-snake.board-test
  (:use clojure.test
        [multi-snake.board :as ms.b]))

(deftest parameterization
  (testing "Default values"
    (let [board (ms.b/make-board)]
      (is (= 20 (:width board)))
      (is (= 20 (:height board)))))
  (testing "Parameterized values"
    (let [board (ms.b/make-board {:width 10 :height 15})]
      (is (= 10 (:width board))
      (is (= 15 (:height board)))))))

