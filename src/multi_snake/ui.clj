(ns multi-snake.ui
  (:require
    ;[seesaw.dev :as ss.dev]
    [multi-snake.game :as ms.g]
    [multi-snake.input :as ms.in]
    [multi-snake.snake :as ms.sn]
    [multi-snake.board :as ms.b]
    [seesaw.border :as ss.b :only line-border]
    [seesaw.core :as ss]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def LOGGING_ENABLED false)
(def ^:dynamic COLOR_MAP {:snakes  [:red :blue]
                          :apples  [:pink :lightblue]
                          :default-apple :black
                          :default-board :white})
(def ^:dynamic *BASE-FPS* 10)
(def ^:dynamic *CELL_SIZE* 25)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Initialize now and then use as a pseudo-singleton throughout
(def FRAME (ss/frame))

; Keep these refs around for easy retrieval
(def XY->CELL (atom {}))

(defn- log
  [& args]
  (when LOGGING_ENABLED
    (apply println (map str args))))

(defn- paint-xy!
  [{:keys [x y]} color]
  (ss/config! (@XY->CELL {:x x :y y}) :background color))

(defn- paint-xys!
  [xys color]
  (dorun (map #(paint-xy! % color) xys)))

(defn- new-cell
  []
  (ss/xyz-panel :border (ss.b/line-border :color :black)))

(defn- paint-snakes!
  [panel snakes]
  (dorun (map-indexed (fn [idx snake]
                        (let [color (get-in COLOR_MAP [:snakes idx])
                              xys (ms.sn/snake-body-as-set snake)]
                          (paint-xys! xys color)))
                      snakes))
  panel)

(defn- paint-apples!
  [panel apples]
  (dorun (map (fn [apple]
                (paint-xy! apple (let [edible-by (get apple :edible-by :all)]
                                   (if (= edible-by :all)
                                     (:default-apple COLOR_MAP)
                                     (get-in COLOR_MAP [:apples edible-by])))))
              apples))
  panel)

(defn- paint-board!
  "Paint all the cells for the given panel"
  [panel {:keys [width height] :as board}]
  (paint-xys! (ms.b/get-all-cells board) (:default-board COLOR_MAP))
  panel)

(defn- paint-panel!
  [board-panel game]
  (-> board-panel
      (paint-board!  (:board game))
      (paint-apples! (:apples game))
      (paint-snakes! (:snakes game))))

(defn- fill-panel!
  "Add physical cell views to the given panel"
  [panel board]
  (doseq [xy (ms.b/get-all-cells board)]
    (let [c (new-cell)]
      (ss/add! panel c)
      (swap! XY->CELL assoc xy c)))
  panel)

(defn- game->jpanel
  [game]
  (let [{:keys [width height] :as board} (:board game)
        panel (ss/grid-panel :columns width :rows height)]
    (-> panel
        (ss/config! :size [(* *CELL_SIZE* width) :by (* *CELL_SIZE* height)])
        (fill-panel! board)
        (paint-panel! game))
    panel))

(defn- update-frame!
  [game]
  (paint-panel! (ss/select FRAME [:#board]) game)
  (ss/repaint! FRAME))

(defn- game->jframe
  [game]
  (-> FRAME
      (ss/config! :title "Multi-Snake!"
                  :content (game->jpanel game)
                  :resizable? false
                  :on-close :exit
                  :id :#board)
      ss/pack!
      ss/show!))

(defn- render-update
  "Make changes to any UI components that may have changed between frames."
  [game]
  (ss/invoke-soon
    (update-frame! game)))

(defn- attach-inputs
  "Set up necessary keyboard handlers and random listeners"
  [game]
  (dorun (map #(ms.in/attach-input % FRAME) (:inputs game))))

(defn- setup-ui
  "Setup UI with necessary components before game is actually kicked off."
  [game]
  (ss/invoke-now ; make sure we are setup before continuing to run the game
    (game->jframe game)))

(defn- show-generic-dialog
  [{:keys [title msg no-title yes-title]}]
  (ss/invoke-now
    (-> (ss/dialog :title title
                   :content msg
                   :option-type :yes-no
                   :options [(ss/action :name no-title
                                        :handler (fn [e] (System/exit 0)))
                             (ss/action :name yes-title
                                        :handler (fn [e] (ss/return-from-dialog e :ok)))])
        (ss/pack!)
        ; workaround seesaw displaying in top-left
        ; https://groups.google.com/forum/#!topic/seesaw-clj/DPdRyrYO800
        (doto (.setLocationRelativeTo FRAME))
        (ss/show!))))

(defn- show-win
  "Congratulatory dialog"
  []
  (show-generic-dialog {:title "You win!"
                        :msg "Not fast enough for you?\nLet's kick it up a notch."
                        :no-title "No Thanks"
                        :yes-title "OK"}))

(defn- play-again-or-exit
  "Modal dialog which asks if the user wants to quit the game, or try again."
  []
  (show-generic-dialog {:title "Game Over"
                        :msg "You died!"
                        :yes-title "Play again"
                        :no-title "Quit"}))

(defn- game-loop
  "Renders a frame, then responds depending on the game status"
  [initial-game]
  (def ^:private FPS *BASE-FPS*)
  (def ^:private level 1)
  (letfn [(fps-for-level [lvl]
            (+ *BASE-FPS* (* (dec lvl) 10)))
          (prepare-game-for-level [game lvl]
            (def level lvl)
            (def FPS (fps-for-level lvl))
            (dorun (map #(ms.in/reset-input %) (:inputs game))))]
    (loop [game initial-game]
      (render-update game)
      (case (:status game)
        :ongoing (do
                   (Thread/sleep (/ 1000 FPS))
                   (recur (ms.g/tick game)))
        :lose (do
                (play-again-or-exit)
                (prepare-game-for-level initial-game 1)
                (recur initial-game))
        :win (do
               (show-win)
               (prepare-game-for-level initial-game (inc level))
               (recur initial-game))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn play-game
  "One-stop-shop"
  [initial-game]
  (doto initial-game
    (setup-ui)
    (attach-inputs)
    (game-loop)))

