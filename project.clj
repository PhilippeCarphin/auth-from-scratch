(defproject clojure-web-app "0.1.0-SNAPSHOT"
  :description "This is me following a tutorial https://www.youtube.com/watch?v=Tq2t9uTTJj0"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.8.0"]]
  :main ^:skip-aot clojure-web-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
