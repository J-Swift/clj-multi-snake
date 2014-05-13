(ns multi-snake.snake-test
  (:use clojure.test)
  (:require [multi-snake.snake :as ms.sn]))

(deftest pos-in-dir
  (let [pos {:x 5 :y 5}
        f @#'ms.sn/pos-in-dir]
    (testing "sanity"
      (is (= {:x 6 :y 5} (f pos :right)))
      (is (= {:x 4 :y 5} (f pos :left)))
      (is (= {:x 5 :y 4} (f pos :up)))
      (is (= {:x 5 :y 6} (f pos :down))))))

(deftest dir-for-input
  (let [f @#'ms.sn/dir-for-input
        verify (fn [dir input expected]
                 (is (= expected (f input dir))))]
    (testing "sanity"
      (verify :right :right :right)
      (verify :right :left :right)
      (verify :right :down :down)
      (verify :right :up :up))))

