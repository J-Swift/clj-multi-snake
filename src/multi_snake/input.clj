(ns multi-snake.input
  (:require [seesaw.keymap :as ss.key]))

(defprotocol AInput
  "Wrapper around user input"
  ; I really wanted to keep the input abstract, but Java precludes it by requiring
  ; focus to handle keyboard input.
  (attach-input [this component] "Attach the input to the provided component")
  (reset-input [this] "Clears out any inputs that may be queued, to facillitate restarting a level")
  (get-action [this game] "Should return one of :up, :down, :left, :right, or nil"))

(defn basic-input
  "Provide a no-op implementation of AInput"
  []
  (reify AInput
    (attach-input [_ _]) ; no-op
    (reset-input [_]) ; no-op
    (get-action [_ _] nil)))

(defn keyboard-input
  "Use the keyboard. Provide a mapping from actions to keys.
  Defaults to:
  { :up 'UP', :left 'LEFT', :down 'DOWN', :right 'RIGHT' }"
  ([] (keyboard-input {}))
  ([{:keys [up left down right] :or {up "UP" left "LEFT" down "DOWN" right "RIGHT"}}]
  (let [key-atom (atom nil)
        set-action (fn [act] (reset! key-atom act))]
    (reify AInput
      (attach-input [_ component]
        (letfn [(k->action [k act]
                  (ss.key/map-key component k (fn [_] (set-action act))
                                  :scope :global))] ; TODO: make this not global
          (k->action up :up)
          (k->action right :right)
          (k->action down :down)
          (k->action left :left)))
      (reset-input [_] (reset! key-atom nil))
      (get-action [_ _] @key-atom)))))

