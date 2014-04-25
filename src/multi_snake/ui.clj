(ns multi-snake.ui
  (:import
    (java.awt Color Dimension GridLayout)
    (javax.swing JFrame JPanel JLabel)))

(def CELL_SIZE 25)

(defn- new-cell
  [n]
  (JLabel. (str n)))

(defn- fill-panel
  [panel num-cells-wide num-cells-high]
  (loop [n (* num-cells-wide num-cells-high)]
    (if (= 0 n)
      panel
      (do
        (.add panel (new-cell n))
        (recur (- n 1))))))

(defn- board->jpanel
  [{:keys [width height]}]
  (let [panel (JPanel. (GridLayout. width height))]
    (doto panel
      (.setBackground Color/RED)
      (.setPreferredSize (Dimension. (* CELL_SIZE width)
                                     (* CELL_SIZE height))))
    (fill-panel panel width height)))

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

