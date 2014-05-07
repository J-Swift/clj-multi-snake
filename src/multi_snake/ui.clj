(ns multi-snake.ui
  (:require
    ;[seesaw.dev :as ss.dev]
    [multi-snake.game :as ms.g]
    [multi-snake.input :as ms.in]
    [seesaw.border :as ss.b :only line-border]
    [seesaw.core :as ss]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def LOGGING_ENABLED false)
(def DEFAULT_COLOR :white)
(def PLAYER_COLOR :black)
(def ^:dynamic *FPS* 15)
(def ^:dynamic *CELL_SIZE* 25)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Initialize now and then use as a pseudo-singleton throughout
(def FRAME (ss/frame))

; Keep these refs around for easy retrieval
(def N->CELL (atom []))

(defn- log
  [& args]
  (if LOGGING_ENABLED
    (apply println (map str args))))

(defn- xy->n
  "Translate from a 2-dimensaional array index into a 1-dimensional variant."
  [x y max-x]
  (+ x (* max-x y)))

(defn- paint-cell!
  [cell is-player-pos?]
  (ss/config! cell :background (if is-player-pos? PLAYER_COLOR DEFAULT_COLOR)))

(defn- new-cell
  []
  (ss/xyz-panel :border (ss.b/line-border :color :black)))

(defn- paint-panel!
  "Paint all the cells for the given panel"
  [panel num-cells-wide num-cells-high player-pos]
  (log "paint-panel")
  (doseq [y (range num-cells-high)
          x (range num-cells-wide)]
    (let [n (xy->n x y num-cells-wide)
          cell (@N->CELL n)
          is-player-pos? (= {:x x :y y} player-pos)]
      (paint-cell! cell is-player-pos?)))
  panel)

(defn- fill-panel!
  "Add physical cell views to the given panel"
  [panel num-cells]
  (log "fill-panel")
  (dotimes [n num-cells]
    (let [cell (new-cell)]
      (ss/add! panel cell)
      (swap! N->CELL assoc n cell)))
  panel)

(defn- game->jpanel
  [game]
  (log "game->jpanel")
  (let [{:keys [width height]} (:board game)
        player-pos (:player-pos game)
        panel (ss/grid-panel :columns width :rows height)]
    (-> panel
        (ss/config! :size [(* *CELL_SIZE* width) :by (* *CELL_SIZE* height)])
        (fill-panel! (* width height))
        (paint-panel! width height player-pos))))

(defn- update-frame!
  [game]
  (log "update-frame")
  (let [{:keys [width height]} (:board game)
        player-pos (:player-pos game)]
    (paint-panel! (ss/select FRAME [:#board]) width height player-pos)
    (ss/repaint! FRAME)))

(defn- game->jframe
  [game]
  (log "game->jframe")
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
  (log "-------------")
  (log "render-update")
  (ss/invoke-soon
    (update-frame! game)))

(defn- attach-inputs
  "Set up necessary keyboard handlers and random listeners"
  [game]
  (log "attach-inputs")
  (-> (:input game)
      (ms.in/attach-input FRAME)))

(defn- setup-ui
  "Setup UI with necessary components before game is actually kicked off."
  [game]
  (log "--------------")
  (log "render-initial")
  (ss/invoke-now ; make sure we are setup before continuing to run the game
    (game->jframe game)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn play-game
  "One-stop-shop"
  [initial-game]
  (setup-ui initial-game)
  (attach-inputs initial-game)
  (loop [game initial-game]
    (render-update game)
    (Thread/sleep (/ 1000 *FPS*))
    (recur (ms.g/tick game))))

