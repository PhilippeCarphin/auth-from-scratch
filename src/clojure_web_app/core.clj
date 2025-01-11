(ns clojure-web-app.core
  (:require [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn app-handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<H1>Hello World</H1>"})

(defn -main
  "Main function of the application"
  [& args]
  (run-jetty app-handler {:port 8080})
)

(defn hello []
  (println "hello"))



