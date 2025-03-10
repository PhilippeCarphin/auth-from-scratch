(defproject clojure-web-app "0.1.0-SNAPSHOT"
  :description "This is me following a tutorial https://www.youtube.com/watch?v=Tq2t9uTTJj0"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.8.0"]
                 [compojure "1.6.2"]
                 [hiccup "2.0.0-RC4"]
                 [crypto-password "0.3.0"]]
  :repl-options {:init-ns clojure-web-app.core
                 :prompt (fn [ns]
                         (str "\033[1;32m"
                              ns "=>"
                              "\033[0m "))}
  :main ^:skip-aot clojure-web-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
