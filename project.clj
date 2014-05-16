(defproject multi-snake "0.1.0-SNAPSHOT"
  :description "This is a unique take on an old game. Instead of controlling a single snake, you must control two simultaneously. Hilarity ensues."
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [seesaw "1.4.4"]] ; UI framework
  :main multi-snake.core
  :aot [multi-snake.core])
