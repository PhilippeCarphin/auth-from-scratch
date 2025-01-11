(ns clojure-web-app.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route])
  (:gen-class))

(defroutes app
  (GET "/" [] "<H1>ROOT</H1>")
  (GET "/other" [] "<H1>OTHER</H1>"))

(defn -main
  "Main function of the application"
  [& args]
  (run-jetty app {:port 8080})
)

(defn hello []
  (println "hello"))
