(ns clojure-web-app.core
  (:require [ring.adapter.jetty]
            [compojure.core]
            [ring.util.codec]
            [hiccup2.core]
            [crypto.password.bcrypt])
  (:gen-class))

(def users-db {"p@c.c" "$2a$11$tf5K7/sRwWDWsI7J4VwZt.9O8p4ImfX/pZm7.pK6q4nRW1w49kSQ6"})
                        ;; The key to the air shield of planet Druidia
                        ;; Also the combination to President Skroob's luggage
(def sessions {})

(defn h1-response [code message]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<h1>" message "</h1>")})

(defn cookie-response [user addr]
  (let [cookie (str user (crypto.password.bcrypt/encrypt user))]
    (def sessions (assoc sessions cookie {:user user :addr addr}))
    {:status 200
     :headers {"Set-Cookie" (str "session=" cookie "; Path=/;  Max-Age=2592000")
               "Content-Type" "text/html; charset=utf-8"}
     :body (str "<h1>Here " user " have a cookie: " cookie "</h1>")}))

(defn get-rhs [expr]
  (get (clojure.string/split expr #"=" 2) 1))

(defn parse-email-password-stream [stream]
  (map get-rhs
       (map ring.util.codec/url-decode
            (clojure.string/split
             (slurp stream) #"&"))))

(defn continue-session [request]
  (let [cookie-header (get-in request [:headers "cookie"])]
    (if (not cookie-header)
      (h1-response 200 "No 'cookie' header in request")
      (let [cookie (get (clojure.string/split cookie-header #"=" 2) 1)]
        (if (not cookie)
          (h1-response 200 (str "You don't have a session cookie"))
          (let [user (get sessions cookie)]
            (if (and cookie user)
              (h1-response 200 (str "Welcome back " user))
              (h1-response 200 (str "You have a cookie but it does not exist as a key in the sessions map")))))))))

(defn remote-addr [request]
  (or (get-in request [:headers "x-forwarded-for"])
      (:remote-addr request)))

(defn signin [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [saved-bcrypt-result (get users-db email nil)]
     (if saved-bcrypt-result
       (if (crypto.password.bcrypt/check password saved-bcrypt-result)
         (cookie-response email (remote-addr request))
         (h1-response 200 (str "Hello " email " you have entered an incorrect password")))
       (h1-response 200 (str "There is no user with email " email " in the database"))))))

(defn register [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [bcrypt-result (crypto.password.bcrypt/encrypt password)]
     (def users-db (assoc users-db email bcrypt-result))
     (h1-response 200 (str "Registration successful: Added association {"
                           email ": " bcrypt-result
                           "} in \"database\" (heavy emphasis on the quotes around database)")))))

(defn session-html-table []
  [:table
   [:tr [:th "Cookie"] [:th "User"] [:th "Address"]]
   (for [[k v] sessions]
     [:tr
      [:td k]
      [:td (:user v)]
      [:td (:addr v)]])])

(defn list-sessions [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str
          (hiccup2.core/html
           [:html
            [:body (session-html-table)]]))})

(compojure.core/defroutes app
  (compojure.core/GET "/list-sessions" args list-sessions)
  (compojure.core/GET "/session"   args continue-session)
  (compojure.core/POST "/signin"   args signin)
  (compojure.core/POST "/register" args register))

(defn -main
  "Main function of the application"
  [& args]
  (println "Running server using run-jetty on port 8989")
  (ring.adapter.jetty/run-jetty app {:port 8989}))
