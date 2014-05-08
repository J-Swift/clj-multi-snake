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
(def ^:dynamic COLOR_MAP {:player  :black
                          :apple   :red
                          :default :white})
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
  (when LOGGING_ENABLED
    (apply println (map str args))))

(defn- xy->n
  "Translate from a 2-dimensional array index into a 1-dimensional variant."
  [x y max-x]
  (+ x (* max-x y)))

(defn- paint-cell!
  [cell type-of-cell]
  (ss/config! cell :background (type-of-cell COLOR_MAP)))

(defn- new-cell
  []
  (ss/xyz-panel :border (ss.b/line-border :color :black)))

(defn- paint-panel!
  "Paint all the cells for the given panel"
  [panel game]
  (log "paint-panel")
  (let [{{:keys [width height]} :board apples :apples} game]
    (doseq [y (range height)
            x (range width)]
      (let [n (xy->n x y width)
            cell (@N->CELL n)
            pos {:x x :y y}]
        (cond
          (get (ms.g/positions-for-player game) pos) (paint-cell! cell :player)
          (get apples pos) (paint-cell! cell :apple)
          :else (paint-cell! cell :default)))))
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
        panel (ss/grid-panel :columns width :rows height)]
    (-> panel
        (ss/config! :size [(* *CELL_SIZE* width) :by (* *CELL_SIZE* height)])
        (fill-panel! (* width height))
        (paint-panel! game))))

(defn- update-frame!
  [game]
  (log "update-frame")
  (paint-panel! (ss/select FRAME [:#board]) game)
  (ss/repaint! FRAME))

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

