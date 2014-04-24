(ns multi-snake.board)

(defn board
  "Generates a board with the given configuartion values. If no configuration
  is provided, a default board will be generated."
  ([] (board {}))
  ([config]
   (let [width (or (:width config) 20)
         height (or (:height config) 20)]
     {:width width
      :height height})))

