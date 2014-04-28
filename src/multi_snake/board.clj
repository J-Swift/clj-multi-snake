(ns multi-snake.board)

(defn make-board
  "Makes a board with the given configuration values:
  
  :width  - width of the board (default 20)
  :height - height of the baord (default 20)"
  ([] (make-board {}))
  ([{:keys [width height] :or {width 20
                               height 20}}]
   {:width width
    :height height}))

