(ns clojure-web-app.core
  (:require [ring.adapter.jetty]
            [compojure.core]
            [ring.util.codec]
            [hiccup2.core]
            [crypto.password.bcrypt])
  (:gen-class))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Uninteresting implementation details
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def top-link [:a {:href "/~pcarphin/auth-from-scratch/index.html"} "Top"])
(defn basic-response [source code message]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str (hiccup2.core/html
               [:h1 (str "Response from " source)]
               [:p message]
               top-link))})
(defn session-no-cookie-response []
  (basic-response "continue-session" 200
                  (str "You don't have a session cookie")))
(defn session-welcome-back-response [user]
  (basic-response "continue-session" 200
                  (str "Welcome back " user)))
(defn session-cookie-present-but-invalid-response []
  (basic-response "continue-session" 200
                  (str "You have a cookie but it does not exist as a key in the sessions map")))
(defn session-no-cookie-header-response []
  (basic-response "continue-session" 200
                  "No 'cookie' header in request"))
(defn signin-incorrect-password-response [email]
  (basic-response "signin" 200
                  (str "Hello " email " you have entered an incorrect password")))
(defn signin-no-such-user-response [email]
  (basic-response "signin" 200
                  (str "There is no user with email " email " in the database")))
(defn register-success-response [email bcrypt-result]
  (basic-response "register" 200
                  (str "Registration successful: Added association {"
                       email ": " bcrypt-result
                       "} in \"database\" (heavy emphasis on the quotes around database)")))

(defn signin-cookie-response [cookie user addr]
  {:status 200
   :headers {"Set-Cookie" (str "session=" cookie "; Path=/;  Max-Age=2592000")
             "Content-Type" "text/html; charset=utf-8"}
   :body (str (hiccup2.core/html
               [:h1 "Response from signin"]
               [:p "Here " [:tt user] " have a cookie: " [:tt cookie]]
               [:p "This cookie corresponds with the following entry in the \"database\""]
               [:table
                [:tr [:th "Session Token"] [:th "User"] [:th "Client address"]]
                [:tr [:td cookie] [:td user] [:td addr]]]
               [:p [:a {:href "list-sessions"} "list-sessions"]]
               [:p top-link]))})

(defn sessions-table-response [sessions]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str (hiccup2.core/html
               [:html [:body [:table [:tr [:th "Cookie"] [:th "User"] [:th "Address"]]
                              (for [[k v] sessions]
                                [:tr [:td k] [:td (:user v)] [:td (:addr v)]])]
                       [:p top-link]]]))})

(defn get-rhs [expr]
  (get (clojure.string/split expr #"=" 2) 1))

(defn parse-email-password-stream [stream]
  (map get-rhs
       (map ring.util.codec/url-decode
            (clojure.string/split
             (slurp stream) #"&"))))

(defn remote-addr [request]
  (or (get-in request [:headers "x-forwarded-for"])
      (:remote-addr request)))

(defn generate-token [user]
  (str user (crypto.password.bcrypt/encrypt user)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Interesting stuff
;; The password for p@c.c
;; -> The key to the air shield of planet Druidia
;;    (Also the combination to President Skroob's luggage)
;; - Every *-response function just generates some ring response maps
;; - The other functions do what they say and their implementation
;;   is not central to this demonstration
;; - if you set a cookie manually with value 'asdfasdf', you will take p@c.c's
;;   session
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Standins for database
(def users-db {"p@c.c" "$2a$11$tf5K7/sRwWDWsI7J4VwZt.9O8p4ImfX/pZm7.pK6q4nRW1w49kSQ6"})
(def sessions-db {"asdfasdf" {:user "p@c.c" :addr "192.168.0.25"}})

(defn add-session-entry-to-database [session-token user addr]
  (def sessions-db (assoc sessions-db session-token {:user user :addr addr})))

;; Route handlers
(defn continue-session [request]
  (let [cookie-header (get-in request [:headers "cookie"])]
    (if (not cookie-header)
      (session-no-cookie-header-response)
      (let [cookie (get (clojure.string/split cookie-header #"=" 2) 1)
            user (get sessions-db cookie)]
        (if (not cookie)
          (session-no-cookie-response)
          (if user
            (session-welcome-back-response user)
            (session-cookie-present-but-invalid-response)))))))

(defn signin [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [saved-bcrypt-result (get users-db email nil)]
     (if (not saved-bcrypt-result)
       (signin-no-such-user-response email)
       (if (not (crypto.password.bcrypt/check password saved-bcrypt-result))
         (signin-incorrect-password-response email)
         (let [session-token (generate-token email)
               addr (remote-addr request)]
           (add-session-entry-to-database session-token email addr)
           (signin-cookie-response session-token email addr)))))))

(defn register [request]
  (let [[email password] (parse-email-password-stream (:body request))]
   (let [bcrypt-result (crypto.password.bcrypt/encrypt password)]
     (def users-db (assoc users-db email bcrypt-result))
     (register-success-response email bcrypt-result))))

(defn list-sessions [request]
  (sessions-table-response sessions-db))

;; Route definitions
(compojure.core/defroutes app
  (compojure.core/GET "/list-sessions" args list-sessions)
  (compojure.core/GET "/session"   args continue-session)
  (compojure.core/POST "/signin"   args signin)
  (compojure.core/GET "/signin"   args signin)
  (compojure.core/POST "/register" args register)
  (compojure.core/GET "/register" args register))

;; Entry point main function
(defn -main
  "Main function of the application"
  [& args]
  (println "Running server using run-jetty on port 8989")
  (ring.adapter.jetty/run-jetty app {:port 8989}))
