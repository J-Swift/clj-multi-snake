(ns multi-snake.input)

(defprotocol AInput
  "Wrapper around user input"
  (get-action [this game] "Should return one of :up, :down, :left, :right, or nil"))

(defn basic-input
  "Provide a no-op implementation of AInput"
  []
  (reify AInput
    (get-action [_ _] nil)))

