(ns multi-snake.board)

(defn contains-point?
  "Check if a point is valid for a given board"
  [{:keys [width height] :as board}
   {:keys [x y] :as pt}]
  (and (< -1 x width)
       (< -1 y height)))

(defn get-all-cells
  "Return a seq of cells for the given board in order left->right, top->bottom"
  [{:keys [width height] :as board}]
  (for [y (range height)
        x (range width)]
    {:x x :y y}))

(defn make-board
  "Makes a board with the given configuration values:
  
  :width  - width of the board (default 20)
  :height - height of the baord (default 20)"
  ([] (make-board {}))
  ([{:keys [width height] :or {width 20
                               height 20}}]
   {:width width
    :height height}))

