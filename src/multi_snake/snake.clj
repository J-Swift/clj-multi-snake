(ns multi-snake.snake)

(def ^:dynamic *segments-per-apple-eaten* 1)

(defn- pos-in-dir
  "Translate a point one unit in the provided direction.
  N.B. Origin is top-left."
  [{:keys [x y]} dir]
  (let [offsets {:right [1 0] :left [-1 0]
                 :down [0 1]  :up [0 -1]}
        [x' y'] (get offsets dir)]
    {:x (+ x x') :y (+ y y')}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn intersects-self?
  "Check if a given snake has looped back around on itself"
  [snake]
  (some #(> % 1) (vals (frequencies (:body snake)))))

(defn eat-apple
  "Add to our internal accumulator of how many segments we've yet to realize."
  [{:keys [to-grow] :as snake}]
  (assoc snake :to-grow (+ to-grow *segments-per-apple-eaten*)))

(defn maybe-move-tail
  "Conditionally shrink body based on if there are unrealized segments."
  [{:keys [body to-grow] :as snake}]
  (if (> to-grow 0)
    (assoc snake :to-grow (dec to-grow))
    (assoc snake :body (pop body))))

(defn move-head
  "Generate snake resulting from moving given snake in given direction. Input is
  assumed to be well-formed."
  [{:keys [head body] :as snake} dir]
  (let [head' (pos-in-dir head dir)]
    (assoc snake :head head' :dir dir :body (conj body head'))))

(defn make-snake
  "Makes a snake with the given configuration values:
  
  :head - starting position
  :dir  - starting direction
  :body - queue of spaces occupied by body (default to head)
  :to-grow - number of body segments yet to be realized (default to 0)"
  [{:keys [head dir body to-grow]
    :or {to-grow 0}}]
  {:head head
   :dir dir
   :status :alive
   :to-grow to-grow
   :body (or body
             (conj (clojure.lang.PersistentQueue/EMPTY) head))})

