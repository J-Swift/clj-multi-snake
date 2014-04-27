(ns multi-snake.ui
  (:require
    [multi-snake.game :as ms.g])
  (:import
    (java.awt Color Dimension GridLayout)
    (javax.swing SwingUtilities JFrame JPanel BorderFactory)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def LOGGING_ENABLED false)
(def FPS 10)
(def CELL_SIZE 25)
(def DEFAULT_COLOR Color/WHITE)
(def PLAYER_COLOR Color/BLACK)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; Initialize now and then use as a pseudo-singleton throughout
(def FRAME (JFrame.))

; Keep these refs around for easy retrieval
(def N->CELL (atom {}))

(defmacro run-on-ui-thread
  [& body]
  `(SwingUtilities/invokeLater (fn [] (do ~@body))))

(defn- log
  [& args]
  (if LOGGING_ENABLED
    (apply println (map str args))))

(defn- xy->n
  "Translate from a 2-dimensaional array index into a 1-dimensional variant."
  [x y max-x]
  (+ x (* max-x y)))

(defn- paint-cell
  [cell is-player-pos?]
  (doto cell
    (.setBackground (if is-player-pos? PLAYER_COLOR DEFAULT_COLOR))))

(defn- new-cell
  []
  (doto (JPanel.)
    (.setBorder (BorderFactory/createLineBorder Color/BLACK))))

(defn- paint-panel
  "Paint all the cells for the given panel"
  [panel num-cells-wide num-cells-high player-pos]
  (log "paint-panel")
  (doseq [y (range num-cells-high)
          x (range num-cells-wide)]
    (let [n (xy->n x y num-cells-wide)
          cell (@N->CELL n)
          is-player-pos? (= {:x x :y y} player-pos)]
      (paint-cell cell is-player-pos?)))
  panel)

(defn- fill-panel
  "Add physical cell views to the given panel"
  [panel num-cells]
  (log "fill-panel")
  (dotimes [n num-cells]
    (let [cell (new-cell)]
      (.add panel cell)
      (swap! N->CELL assoc n cell)))
  panel)

(defn- game->jpanel
  [game]
  (log "game->jpanel")
  (let [{:keys [width height]} (:board game)
        player-pos (:player-pos game)
        panel (JPanel. (GridLayout. width height) true)]
    (doto panel
      (.setPreferredSize (Dimension. (* CELL_SIZE width)
                                     (* CELL_SIZE height)))
      (fill-panel (* width height))
      (paint-panel width height player-pos))))

(defn- update-frame
  [game]
  (log "update-frame")
  (let [{:keys [width height]} (:board game)
        player-pos (:player-pos game)]
    (paint-panel (.getComponent FRAME 0) width height player-pos)
    (doto FRAME
      (.repaint))))

(defn- game->jframe
  [game]
  (log "game->jframe")
  (doto FRAME
    (.add (game->jpanel game))
    (.pack)
    (.setTitle "Multi-Snake!")
    (.setResizable false)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.setVisible true)))

(defn- render-update
  "Make changes to any UI components that may have changed between frames."
  [game]
  (log "-------------")
  (log "render-update")
  (run-on-ui-thread
    (update-frame game)))

(defn- setup-ui
  "Setup UI with necessary components before game is actually kicked off."
  [game]
  (log "--------------")
  (log "render-initial")
  (run-on-ui-thread
    (game->jframe game)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn play-game
  "One-stop-shop"
  [initial-game]
  (setup-ui initial-game)
  (loop [game initial-game]
    (render-update game)
    (Thread/sleep (/ 1000 FPS))
    (recur (ms.g/tick game))))

