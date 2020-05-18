(defproject timeline "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]

                 [org.clojure/test.check "0.10.0-alpha3" :scope "test"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [org.clojure/core.specs.alpha "0.1.24"]

                 [cljsjs/d3 "5.12.0-0"]
                 [cljs-bean "1.5.0"]]
  :source-paths ["src"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
            "fig:test"  ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "timeline.test-runner"]}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.5"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]
                                  [org.clojure/test.check "0.10.0"]]}})
