(ns multi-snake.ui
  (:import
    (java.awt Color Dimension)
    (javax.swing JFrame JPanel)))

(def CELL_SIZE 25)

(defn- board->jpanel
  [board]
  (doto (JPanel.)
    (.setBackground Color/RED)
    (.setPreferredSize (Dimension. (* CELL_SIZE (:width board))
                                   (* CELL_SIZE (:height board))))))

(defn- board->jframe
  [board]
  (doto (JFrame. "Multi-Snake!")
    (.add (board->jpanel board))
    (.pack)
    (.setResizable false)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)))

(defn render
  [board]
  (let [frame (board->jframe board)]
    (.setVisible frame true))
  nil)

