(ns clojure-web-app.core
  (:require [ring.adapter.jetty]
            [compojure.core]
            [ring.util.codec]
            [crypto.password.bcrypt])
  (:gen-class))

(def users-db {"p@c.c" "$2a$11$tf5K7/sRwWDWsI7J4VwZt.9O8p4ImfX/pZm7.pK6q4nRW1w49kSQ6"})
                        ;; The key to the air shield of planet Druidia
                        ;; Also the combination to President Skroob's luggage

(defn h1-response [code message]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<h1>" message "</h1>")})

(defn get-rhs [expr]
  (get (clojure.string/split expr #"=" 2) 1))

(defn parse-email-password-stream [stream]
  (map get-rhs
       (map ring.util.codec/url-decode
            (clojure.string/split
             (slurp stream) #"&"))))

(defn signin [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [saved-bcrypt-result (get users-db email nil)]
     (if saved-bcrypt-result
       (if (crypto.password.bcrypt/check password saved-bcrypt-result)
         (h1-response 200 (str "Hello " email " you have been successfully authenticated"))
         (h1-response 200 (str "Hello " email " you have entered an incorrect password")))
       (h1-response 200 (str "There is no user with email " email " in the database"))))))

(defn register [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [bcrypt-result (crypto.password.bcrypt/encrypt password)]
     (def users-db (assoc users-db email bcrypt-result))
     (h1-response 200 (str "Registration successful: Added association {"
                           email ": " bcrypt-result
                           "} in \"database\" (heavy emphasis on the quotes around database)")))))

(compojure.core/defroutes app
  (compojure.core/POST "/signin"   args signin)
  (compojure.core/POST "/register" args register))

(defn -main
  "Main function of the application"
  [& args]
  (println "Running server using run-jetty on port 8989")
  (ring.adapter.jetty/run-jetty app {:port 8989}))
