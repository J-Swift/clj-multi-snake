(ns multi-snake.snake)

(def ^:dynamic *segments-per-apple-eaten* 1)

(defn- pos-in-dir
  "Translate a point one unit in the provided direction.
  N.B. Origin is top-left."
  [{:keys [x y]} dir]
  {:pre (#{:right :down :left :up} dir)}
  (let [offsets {:right [1 0] :left [-1 0]
                 :down [0 1]  :up [0 -1]}
        [x' y'] (get offsets dir)]
    {:x (+ x x') :y (+ y y')}))

(defn- dir-for-input
  "Either return the action if it is valid or the cur-dir if not."
  [input cur-dir]
  (let [opposites #{[:up :down], [:left :right]
                    [:down :up], [:right :left]}]
    (if (and input
             (not (opposites [input cur-dir])))
      input
      cur-dir)))

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
  "Generate snake resulting from moving only the head according to provided input."
  [{:keys [head body dir] :as snake} input]
  (let [dir' (dir-for-input input dir)
        head' (pos-in-dir head dir')]
    (assoc snake :head head' :dir dir' :body (conj body head'))))

(defn snake-body-as-set
  [{:keys [body] :as snake}]
  (into #{} body))

(defn make-snake
  "Makes a snake with the given configuration values:
  
  :head    - starting position (default {0,0})
  :dir     - starting direction (default :right)
  :body    - queue of spaces occupied by body (default to head)
  :to-grow - number of body segments yet to be realized (default to 0)"
  ([] (make-snake {}))
  ([{:keys [head dir body to-grow]
    :or {head {:x 0 :y 0}
         dir :right
         to-grow 0}}]
  {:post [(some #{(:head %)}
                (:body %))]} ; ensure the head is included in the body
  {:head head
   :dir dir
   :status :alive
   :to-grow to-grow
   :body (or body
             (conj (clojure.lang.PersistentQueue/EMPTY) head))}))

